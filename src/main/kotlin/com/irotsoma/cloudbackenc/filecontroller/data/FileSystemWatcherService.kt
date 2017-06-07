/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
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
package com.irotsoma.cloudbackenc.filecontroller.data

import com.irotsoma.cloudbackenc.common.compression.BzipFile
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.encryption.EncryptionServiceRepository
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
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
import java.util.concurrent.TimeUnit
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

    @Autowired
    lateinit var watchedLocationRepository: WatchedLocationRepository
    @Autowired
    lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    lateinit var storedFileRepository: StoredFileRepository
    @Autowired
    lateinit var encryptionServiceRepository: EncryptionServiceRepository

    var watchService: WatchService? = null

    private val filesToUpdate = LinkedBlockingQueue<UUID>()

    @Volatile private var keepRunning = false
    var sendFilesStatus: Future<Any>? = null

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
                            if (watchedLocation.recursive ?: false) {
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
                    if ((watchedFile is File) && (!watchedFile.isDirectory)) {
                        if (watchedFile.canRead()){
                            //register the path with the watch service
                            watchedFile.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)
                            var storedFile = storedFileRepository.findByPathAndWatchedLocationUuid(watchedFile.absolutePath, watchedLocation.uuid)
                            //add the file to the database if it doesn't exist
                            if (storedFile == null) {
                                storedFile = StoredFile(UUID.randomUUID(), watchedLocation.uuid, watchedFile.absolutePath, null, Date(0L),watchedLocation.encryptionServiceUuid, watchedLocation.encryptionIsSymmetric, watchedLocation.secretKey, watchedLocation.publicKey, watchedLocation.iv)
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
        sendFilesStatus = sendFileRequests()
    }

    @Async
    @Scheduled(fixedDelayString="\$filecontroller.poll.frequency")
    private fun pollWatchers() {
        if (keepRunning) {
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
                }
                watchKey = watchService?.poll()
            }
        }
    }

    @Async
    private fun sendFileRequests(): Future<Any>?{
        val centralControllerProtocol = if (centralControllerSettings.useSSL) "https" else "http"
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }

        while (keepRunning){
            //poll times out every POLL_TIMEOUT milliseconds to check to see if it should keep waiting
            val fileUuid = filesToUpdate.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS)
            if (fileUuid != null) {
                //TODO: compress and encrypt file and add/update database entry
                //TODO: get version of file back from central controller and store hashes of original and encrypted file
                val storedFile = storedFileRepository.findByUuid(fileUuid)
                if (storedFile != null) {

                    val compressedFile = File.createTempFile(FilenameUtils.getName(storedFile.path), ".tmp")
                    BzipFile().compressFile(File(storedFile.path), compressedFile)
                    val encryptionServiceUuid = storedFile.encryptionServiceUuid ?: encryptionServiceRepository.encryptionServicesSettings.defaultServiceUuid
                    val watchedLocation = watchedLocationRepository.findByUuid(storedFile.watchedLocationUuid)



                    //TODO: encryption here
                    //encryptionServiceRepository.encryptionServiceExtensions.get()








                    val centralControllerURL = "$centralControllerProtocol://${centralControllerSettings.host}:${centralControllerSettings.port}/files"
                    val requestHeaders = HttpHeaders()
                    requestHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ${watchedLocation?.user?.userToken}")
                    val requestParameters = LinkedMultiValueMap<String, Any>()
                    requestParameters.add("file", ClassPathResource(storedFile.path))
                    requestParameters.add("uuid", fileUuid)
                    val httpEntity = HttpEntity<LinkedMultiValueMap<String, Any>>(requestParameters, requestHeaders)
                    val callResponse = RestTemplate().postForEntity(centralControllerURL, httpEntity, UUID::class.java)
                    if (callResponse.statusCode == HttpStatus.OK) {
                        logger.debug { "File with local UUID $fileUuid uploaded and assigned remote UUID ${callResponse.body}" }
                    } else {
                        logger.warn { "Error sending file with local UUID $fileUuid and path ${storedFile.path}" }
                        logger.warn { "Server returned ${callResponse.statusCode.name}" }
                    }
                } else{

                    //TODO: handle stored file uuid is not in DB
                }
            }
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
            while (sendFilesStatus?.isDone == false) {
                Thread.sleep(1000L)
                waitLimitTimer++
                if (waitLimitTimer > 59){
                    //if 59 seconds pass then reset the keepRunning flag to true to let the processes continue, wait one more second to be sure it actually didn't exit in the moment the keepRunning value was set and then return false
                    keepRunning = true
                    Thread.sleep(1000L)
                    if (sendFilesStatus?.isDone == false) {
                        //current transfer is taking a while so the caller should try again later
                        return false
                    } else {
                        //oops, looks like the function quit before it registered the keep running flag so let's restart as normal
                        keepRunning = false
                    }
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