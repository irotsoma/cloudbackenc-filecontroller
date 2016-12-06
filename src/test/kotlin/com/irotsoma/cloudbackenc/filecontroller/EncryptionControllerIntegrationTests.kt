/*
 * Created by irotsoma on 11/14/16.
 */
package com.irotsoma.cloudbackenc.filecontroller

import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceFileRequest
import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.EncryptionServiceSymmetricEncryptionAlgorithms
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.File
import java.security.MessageDigest
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
open class EncryptionControllerIntegrationTests {
    @LocalServerPort
    private var port: Int = 0
    @Value("\${server.ssl.key-store}")
    private var useSSL: String? = null
    var protocol: String = "http"

    //only valid when bouncy castle encryption extension is installed
    @Test
    fun testEncryptDecryptFile() {
        val restTemplate: TestRestTemplate
        if (useSSL != null && useSSL != "") {
            protocol = "https"
            trustSelfSignedSSL()
            restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.SSL)
        } else {
            protocol = "http"
            restTemplate = TestRestTemplate()
        }
        val decodedKey = Base64.getDecoder().decode("000102030405060708090a0b0c0d0e0f")
        val testKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        val requestHeadersEncrypt = HttpHeaders()
        requestHeadersEncrypt.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        val requestEncrypt = EncryptionServiceFileRequest(javaClass.classLoader.getResource("TestEncryptFile.dat").path, EncryptionServiceSymmetricEncryptionAlgorithms.AES,testKey, null, null)
        val httpEntityEncrypt = HttpEntity<EncryptionServiceFileRequest>(requestEncrypt, requestHeadersEncrypt)
        val responseEncrypt = restTemplate.postForObject("$protocol://localhost:$port/file-encryptors/8ccdef5f-5833-4264-acd5-4c67a24320c0/encrypted-file", httpEntityEncrypt, Any::class.java)
        val encryptedFile = File.createTempFile("encryptedfile_",".dat")
        //FileCopyUtils.copy(responseEncrypt.body, encryptedFile)

        val decryptedFile = File.createTempFile("decryptedfile_",".dat")
        val requestMapDecrypt = LinkedMultiValueMap<String, Any>()
        val requestDecrypt = EncryptionServiceFileRequest(decryptedFile.absolutePath,EncryptionServiceSymmetricEncryptionAlgorithms.AES,testKey, null, null)
        requestMapDecrypt.add("metadata",requestDecrypt)
        val mockFile = MockMultipartFile("TestEncryptedFile.dat", encryptedFile.inputStream())
        requestMapDecrypt.add("file", mockFile)
        val httpEntityDecrypt = HttpEntity<MultiValueMap<String,Any>>(requestMapDecrypt)
        restTemplate.put("$protocol://localhost:$port/file-encryptors/8ccdef5f-5833-4264-acd5-4c67a24320c0/decrypted-file",httpEntityDecrypt)

        assert(hashFile(File(javaClass.classLoader.getResource("TestEncryptFile.dat").path)) == hashFile(decryptedFile))

    }
    fun hashFile(file: File): String{
        val messageDigest = MessageDigest.getInstance("SHA1")
        val decryptedFileInputStream = file.inputStream()
        val dataBytes = ByteArray(1024)
        var readBytes = decryptedFileInputStream.read(dataBytes)
        while (readBytes > -1){
            messageDigest.update(dataBytes,0,readBytes)
            readBytes = decryptedFileInputStream.read(dataBytes)
        }
        val outputBytes: ByteArray = messageDigest.digest()
        return DatatypeConverter.printHexBinary(outputBytes)
    }
}