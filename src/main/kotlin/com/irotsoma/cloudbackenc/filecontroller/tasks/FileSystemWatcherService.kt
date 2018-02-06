/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

/*
 * Created by irotsoma on 4/26/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.tasks

import com.irotsoma.cloudbackenc.common.AuthenticationToken
import com.irotsoma.cloudbackenc.common.Utilities.hashFile
import com.irotsoma.cloudbackenc.filecontroller.BzipFile
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.data.*
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


/**
 *
 *
 * @author Justin Zak
 */

@Component
class FileSystemWatcherService {
    //data class QueueItem(val uuid: UUID, val watchedLocationUuid:UUID, val path: Path, val token: String)
    /** kotlin-logging implementation*/
    companion object: KLogging(){
        /** Time to wait for a new poll event before checking if the service is shutting down. */
        private const val POLL_TIMEOUT = 5000L
    }

    @Autowired lateinit var watchedLocationRepository: WatchedLocationRepository
    @Autowired lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired lateinit var storedFileRepository: StoredFileRepository
    @Autowired lateinit var storedFileVersionRepository: StoredFileVersionRepository
    @Autowired lateinit var centralControllerUserRepository: CentralControllerUserRepository
   /* @Autowired
    lateinit var encryptionExtensionRepository: EncryptionExtensionRepository
*/
    @Value("\${filecontroller.frequencies.recheckblacklist}")
    private var recheckBlacklistFrequency = 600000L

    @Volatile private var watchService: WatchService? = null
    @Volatile private var filesToUpdate = LinkedBlockingQueue<UUID>()
    @Volatile private var keepRunning = false
    @Volatile private var runningSendProcesses = mutableListOf<UUID>()

    val userBlacklist = HashMap<String, Date>()

    @PostConstruct
    fun initializeService(){
        watchService = FileSystems.getDefault().newWatchService()
        watchedLocationRepository.findAll()
            .forEach { watchedLocation ->
                val fileList = mutableListOf<File>()
                val filter = WildcardFileFilter(if (watchedLocation.filter.isNullOrBlank()) {"*"} else{ watchedLocation.filter })
                val watchedLocationFile = File(watchedLocation.path)
                if (watchedLocationFile.isDirectory) {
                    val files =
                            if (watchedLocation.recursive == true) {
                                FileUtils.listFiles(File(watchedLocation.path), filter, TrueFileFilter.INSTANCE)
                            } else {
                                FileUtils.listFiles(File(watchedLocation.path), filter, null)
                            }
                    files
                            .filterIsInstance<File>()
                            .filterTo(fileList) { it.exists() }
                } else {
                    val file = File(watchedLocation.path)
                    if (file.exists()){
                        fileList.add(file)
                    }
                }
                fileList.forEach { watchedFile ->
                    if (!watchedFile.isDirectory) {
                        if (watchedFile.canRead()){
                            //register the path with the watch service
                            watchedFile.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
                            var storedFile = storedFileRepository.findByPathAndWatchedLocationUuid(watchedFile.absolutePath, watchedLocation.uuid)
                            //add the file to the database if it doesn't exist
                            if (storedFile == null) {
                                storedFile = StoredFile(UUID.randomUUID(), watchedLocation, watchedFile.absolutePath, Date(0L))
                                storedFileRepository.save(storedFile)
                            }

                            //if the last modified date is greater than the one in the database then add the file to the queue to be updated at the cloud service
                            if (watchedFile.lastModified() > storedFile.lastUpdated.time) {
                                filesToUpdate.put(storedFile.uuid)
                            }
                        } else {
                            logger.debug { "Unable to access registered path: $watchedFile" }
                        }
                    }
                }
            }
        keepRunning = true
    }

    /**
     * Periodically polls the file system watchers and adds any changed files to a queue to be sent to the central controller
     */
    @Scheduled(initialDelay = 1000, fixedDelay=POLL_TIMEOUT)
    fun pollWatchers(): Future<Any>? {
        var filesChanged = 0L
        if (keepRunning) {
            val processUuid = UUID.randomUUID()
            logger.trace{"Async process -- pollWatcher $processUuid: Executing poll watcher function."  }
            var watchKey = watchService?.poll()
            while (watchKey != null) {
                watchKey.pollEvents().forEach { event ->
                    //remove duplicates and save to be processed
                    if (event.kind().type() is Path) {
                        val path = event.context() as Path
                        //find all users watching this file and add the appropriate items to the queue
                        storedFileRepository.findByPath(path.toAbsolutePath().toString()).forEach { file ->
                            //remove already queued instances of this work item to help with files that are being changed often making it wait until later if there are other queued items
                            filesToUpdate.removeIf { uuid -> uuid == file.uuid }
                            filesToUpdate.put(file.uuid)
                        }
                    }
                    filesChanged++
                }
                watchKey = watchService?.poll()
            }
            logger.trace {"Async process -- pollWatcher $processUuid: Process finished.  Files added to queue: $filesChanged."}
        }
        return null
    }

    /**
     * Cycles through any blacklisted user IDs if they were blacklisted more than the configured interval ago.
     * Tries to get a new user token from the central controller and if successful with the current token, removes the user from the blacklist.
     * The token would need to be externally updated in order for this to happen.
     */
    @Scheduled(fixedDelayString = "\${filecontroller.frequencies.recheckblacklist}")
    fun recheckBlacklist(): Future<Any>?{
        val centralControllerProtocol = if (centralControllerSettings.useSSL) "https" else "http"
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "Central Controller SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val processUuid = UUID.randomUUID()
        logger.trace{"Async process -- recheckBlackList $processUuid: Running process to check for updated credentials."  }
        for(entry in userBlacklist) {
            if (Date().time >= entry.value.time + recheckBlacklistFrequency) {
                val user = centralControllerUserRepository.findByUsername(entry.key)
                if (user != null) {
                    val centralControllerURL = "$centralControllerProtocol://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.authPath}/token"
                    val requestHeaders = HttpHeaders()
                    requestHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ${user.token}")
                    val httpEntity = HttpEntity<Any>(requestHeaders)
                    val response : ResponseEntity<AuthenticationToken>? =
                        try {
                            RestTemplate().exchange(centralControllerURL, HttpMethod.GET, httpEntity, AuthenticationToken::class.java)
                        } catch (ignore: Exception) { null }
                    if (response?.body?.token != null){
                        user.token = response.body?.token
                        centralControllerUserRepository.save(user)
                        userBlacklist.remove(entry.key)
                    }
                }
            }
        }
        logger.trace {"Async process -- recheckBlackList $processUuid: Process finished.  Users still blacklisted: ${userBlacklist.size}."}
        return null
    }

    /**
     * Periodically attempts to send any queued files to the central controller for storing on a cloud service provider.
     */
    @Scheduled(fixedDelayString="\${filecontroller.frequencies.filepoll}")
    fun sendFileRequests(): Future<Any>?{
        if (keepRunning){
            var filesSent = 0L
            val processUuid = UUID.randomUUID()
            runningSendProcesses.add(processUuid)
            logger.trace{"Async process -- sendFileRequests $processUuid: Executing Send File Requests function."  }
            val centralControllerProtocol = if (centralControllerSettings.useSSL) "https" else "http"
            //for testing use a hostname verifier that doesn't do any verification
            if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
                trustSelfSignedSSL()
                logger.warn { "Central Controller SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
            }
            //val encryptionFactoryClasses = HashMap<UUID, EncryptionFactory?>()
            //val secureRandom = SecureRandom.getInstanceStrong()

            logger.trace{"Async process -- sendFileRequests $processUuid: Sending any changed files."}
            val fileUuid = filesToUpdate.poll()
            if (fileUuid != null) {
                val storedFile = storedFileRepository.findByUuid(fileUuid)
                if (storedFile != null) {
                    val watchedLocation = storedFile.watchedLocation
                    if (watchedLocation.user.username !in userBlacklist.keys) {

                        /* moving encryption to central controller
                        //load factory if it hasn't already been loaded
                        val encryptionUuid = watchedLocation.encryptionUuid ?: UUID.fromString(encryptionExtensionRepository.encryptionExtensionSettings.defaultExtensionUuid)
                        if (!encryptionFactoryClasses.containsKey(encryptionUuid)) {
                            encryptionFactoryClasses.put(encryptionUuid, encryptionExtensionRepository.extensions[encryptionUuid]?.newInstance() as EncryptionFactory?)
                            if (encryptionFactoryClasses[encryptionUuid] == null) {
                                logger.warn { "Unable to load encryption service factory with UUID: $encryptionUuid.  Files using this service will not be processed." }
                            }
                        }
                        val encryptionKey: Key
                        val encryptionAlgorithm: EncryptionAlgorithms
                        if (watchedLocation.encryptionIsSymmetric) {
                            encryptionAlgorithm = EncryptionSymmetricEncryptionAlgorithms.valueOf(watchedLocation.encryptionAlgorithm)
                            val decodedKey = Base64.getDecoder().decode(watchedLocation.secretKey)
                            encryptionKey = SecretKeySpec(decodedKey, watchedLocation.encryptionKeyAlgorithm)
                        } else {
                            encryptionAlgorithm = EncryptionAsymmetricEncryptionAlgorithms.valueOf(watchedLocation.encryptionAlgorithm)
                            val decodedKey = Base64.getDecoder().decode(watchedLocation.publicKey)
                            val x509publicKey = X509EncodedKeySpec(decodedKey)
                            encryptionKey = KeyFactory.getInstance(watchedLocation.encryptionKeyAlgorithm).generatePublic(x509publicKey)
                        }
                        if (encryptionFactoryClasses[encryptionUuid] != null) {
                            val encryptedFile = File.createTempFile(FilenameUtils.getName(storedFile.path), ".enc.tmp")
                            var ivParameterSpec: IvParameterSpec? = null
                            if (watchedLocation.encryptionBlockSize != -1){
                                val byteArray = ByteArray(watchedLocation.encryptionBlockSize)
                                secureRandom.nextBytes(byteArray)
                                ivParameterSpec =  IvParameterSpec(byteArray)
                            }


                            encryptionFactoryClasses[encryptionUuid]!!.encryptionFileService.encrypt(compressedFile.inputStream(), encryptedFile.outputStream(), encryptionKey, encryptionAlgorithm, ivParameterSpec, secureRandom)

                            val encryptedHash = hashFile(encryptedFile)
                            */
                            val hash = hashFile(File(storedFile.path))
                            val compressedFile = File.createTempFile(FilenameUtils.getName(storedFile.path), ".bz2")
                            val inputFile = File(storedFile.path)
                            BzipFile().compressFile(inputFile, compressedFile)
                            val centralControllerURL = "$centralControllerProtocol://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.filesPath}"
                            //TODO: better user token system
                            val requestHeaders = HttpHeaders()
                            requestHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ${watchedLocation.user.token}")
                            val requestParameters = LinkedMultiValueMap<String, Any>()
                            requestParameters.add("file", ClassPathResource(compressedFile.absolutePath))
                            requestParameters.add("uuid", fileUuid)
                            val httpEntity = HttpEntity(requestParameters, requestHeaders)
                            val callResponse = RestTemplate().postForEntity(centralControllerURL, httpEntity, Pair::class.java)

                            if (callResponse.statusCode == HttpStatus.OK) {
                                val fileVersion = try {callResponse.body?.second as Long } catch (ignore:Exception) {null}
                                val fileRemoteUUID = try {callResponse.body?.first as UUID} catch (ignore:Exception){null}
                                if (fileVersion == null || fileRemoteUUID == null){
                                    logger.error{"Invalid Remote UUID or version number returned from central controller. Values: ${callResponse.body?.first}, ${callResponse.body?.second}"}
                                } else {
                                    logger.trace { "File with local UUID $fileUuid uploaded and assigned remote UUID ${callResponse.body?.first}" }

                                    storedFile.lastUpdated = Date(inputFile.lastModified())
                                    //val newVersion = StoredFileVersion(storedFile.uuid, fileRemoteUUID, fileVersion, ivParameterSpec?.iv, watchedLocation.encryptionUuid, watchedLocation.encryptionIsSymmetric, watchedLocation.encryptionAlgorithm, watchedLocation.encryptionKeyAlgorithm, watchedLocation.encryptionBlockSize, watchedLocation.secretKey, watchedLocation.publicKey, Date(), originalHash, encryptedHash)
                                    val newVersion = StoredFileVersion(storedFile,fileRemoteUUID,fileVersion, hash, Date())
                                    storedFileVersionRepository.saveAndFlush(newVersion)
                                    filesSent++
                                }

                            } else if(callResponse.statusCode == HttpStatus.UNAUTHORIZED) {
                                //add user to blacklist so their files will not be processed until their credentials are updated.
                                userBlacklist.put(watchedLocation.user.username, Date())
                                logger.debug { "Error sending file with local UUID $fileUuid and path ${storedFile.path}" }
                                logger.debug { "Server returned ${ callResponse.statusCode.name }" }
                                logger.debug { "User ${watchedLocation.user.username} has been temporarily blacklisted."}
                            } else {
                                logger.warn { "Error sending file with local UUID $fileUuid and path ${storedFile.path}" }
                                logger.warn { "Server returned ${callResponse.statusCode.name}" }
                            }
//                        } else {
//                            logger.warn { "Skipping file with UUID $fileUuid because the encryption service with UUID $encryptionUuid failed to load." }
//                            logger.warn { "Fix the encryption service plugin and restart the service to process these files." }
//                        }
                    } else {
                        logger.debug{"Skipping file with local UUID $fileUuid and path ${storedFile.path}: User ${watchedLocation.user.username} is blacklisted."}
                    }
                } else {

                    logger.error{"Queued file with local UUID $fileUuid is missing from the database. This file will be skipped."}
                }
                logger.trace {"Async process -- sendFileRequests $processUuid: Process ending.  Files sent to central controller: $filesSent."}
            } else {
                logger.trace {"Async process -- sendFileRequests $processUuid: No files to send."}
            }
            runningSendProcesses.remove(processUuid)
        }
        return null
    }

    @PreDestroy
    private fun destroyService(){
        keepRunning = false

    }

    /**
     * Rebuilds the watch service and restarts the async transfer service
     *
     * @return Returns false if the service is in the process of being destroyed or the transfer process doesn't shut down in 60 seconds
     */
    fun reloadWatchServices(): Boolean{
        if (keepRunning) {
            keepRunning = false
            //wait for file transfer process to finish latest item or 59 seconds pass
            var waitLimitTimer = 0
            while (runningSendProcesses.isNotEmpty()) {
                Thread.sleep(1000L)
                waitLimitTimer++
                if (waitLimitTimer > 60){
                    //if 60 seconds pass then reset the keepRunning flag to true to let the processes continue
                    keepRunning = true
                    //current transfer is taking a while so the caller should try again later
                    return false
                }
            }
            initializeService()
            return true
        } else {
            //service is already in the process of being shut down and can't be restarted
            return false
        }
    }
}