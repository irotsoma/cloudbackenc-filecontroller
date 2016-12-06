/*
 * Created by irotsoma on 11/11/16.
 */
package com.irotsoma.cloudbackenc.filecontroller.controllers

import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceAsymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceFileRequest
import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceSymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.filecontroller.controllers.compression.BzipFile
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.FileNotWritatbleException
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.InvalidEncryptionServiceUUIDException
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.UnsupportedEncryptionAlgorithmException
import com.irotsoma.cloudbackenc.filecontroller.encryption.EncryptionServiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*

@Controller
@RequestMapping("/file-encryptors/{uuid}/decrypted-file")
class GetDecryptedFileController {

    @Autowired
    private lateinit var encryptionServiceRepository: EncryptionServiceRepository

    @RequestMapping(method = arrayOf(RequestMethod.PUT))
    @ResponseBody fun getDecryptedFile(@PathVariable(value="uuid")uuid: UUID, @RequestParam("metadata") request: EncryptionServiceFileRequest, @RequestParam("file") file: MultipartFile) {
        //verify that file exists
        val decryptedFile = File(request.filePath)
        if (!decryptedFile.canWrite()){
            throw FileNotWritatbleException()
        }
        val encryptionServiceClass = encryptionServiceRepository.encryptionServiceExtensions[uuid]?.newInstance()  ?: throw InvalidEncryptionServiceUUIDException()
        val fileEncryptionService = encryptionServiceClass.encryptionServiceFileService

        //check to see if the service supports the requested encryption algorithm
        if ((request.algorithm is EncryptionServiceSymmetricEncryptionAlgorithms) && (request.algorithm !in encryptionServiceClass.supportedSymmetricEncryptionAlgorithms) ){
            throw UnsupportedEncryptionAlgorithmException()
        } else if (((request.algorithm is EncryptionServiceAsymmetricEncryptionAlgorithms) && (request.algorithm !in encryptionServiceClass.supportedAsymmetricEncryptionAlgorithms) )){
            throw UnsupportedEncryptionAlgorithmException()
        }
        val outputFile = File.createTempFile(file.name,".tmp")
        fileEncryptionService.decrypt(file.inputStream, outputFile.outputStream(), request.key, request.algorithm, request.ivParameterSpec, request.secureRandom)
        val bzipCompression =  BzipFile()
        bzipCompression.decompressFile(outputFile, decryptedFile)
    }
}