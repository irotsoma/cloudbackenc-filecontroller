/*
 * Copyright (C) 2016  Irotsoma, LLC
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
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
/*
 * Created by irotsoma on 10/31/16.
 */
package com.irotsoma.cloudbackenc.filecontroller.controllers

import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceAsymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceSymmetricEncryptionAlgorithms
import com.irotsoma.cloudbackenc.filecontroller.controllers.compression.BzipFile
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.EncryptionServiceFileNotFoundException
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.InvalidEncryptionServiceUUIDException
import com.irotsoma.cloudbackenc.filecontroller.controllers.exceptions.UnsupportedEncryptionAlgorithmException
import com.irotsoma.cloudbackenc.filecontroller.encryption.EncryptionServiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import java.io.File
import java.nio.file.Files
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/files/")
class EncryptedFileController {
    @Autowired
    private lateinit var encryptionServiceRepository: EncryptionServiceRepository

    @RequestMapping(method = arrayOf(RequestMethod.GET))
    fun getEncryptedFile(@RequestParam request: String, response: HttpServletResponse) {
        //verify that file exists
        val fileToEncrypt = File(request.filePath)
        if (!fileToEncrypt.exists()){
            throw EncryptionServiceFileNotFoundException()
        }







        val encryptionServiceClass = encryptionServiceRepository.encryptionServiceExtensions[uuid]?.newInstance()  ?: throw InvalidEncryptionServiceUUIDException()
        val fileEncryptionService = encryptionServiceClass.encryptionServiceFileService

        //check to see if the service supports the requested encryption algorithm
        if ((request.algorithm is EncryptionServiceSymmetricEncryptionAlgorithms) && (request.algorithm !in encryptionServiceClass.supportedSymmetricEncryptionAlgorithms) ){
            throw UnsupportedEncryptionAlgorithmException()
        } else if (((request.algorithm is EncryptionServiceAsymmetricEncryptionAlgorithms) && (request.algorithm !in encryptionServiceClass.supportedAsymmetricEncryptionAlgorithms) )){
            throw UnsupportedEncryptionAlgorithmException()
        }
        val outputFile = File.createTempFile(fileToEncrypt.name,".tmp")
        val compressedFile = File.createTempFile("${fileToEncrypt.name}.bz2",".tmp")
        val bzipCompression =  BzipFile()
        bzipCompression.compressFile(fileToEncrypt, compressedFile)
        fileEncryptionService.encrypt(compressedFile.inputStream(), outputFile.outputStream(), request.key, request.algorithm, request.ivParameterSpec, request.secureRandom)
        Files.copy(outputFile.toPath(), response.outputStream)
        response.flushBuffer()
        outputFile.delete()
        compressedFile.delete()
    }

    //@RequestMapping(method = arrayOf(RequestMethod.PUT))

}