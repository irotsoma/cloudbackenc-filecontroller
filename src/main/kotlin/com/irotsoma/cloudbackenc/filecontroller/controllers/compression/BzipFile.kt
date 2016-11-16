/*
 * Created by irotsoma on 11/14/16.
 */
package com.irotsoma.cloudbackenc.filecontroller.controllers.compression

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.*


class BzipFile {
    companion object{
        val bufferSize = 2048
    }
    fun decompressFile(inputFile: File, outputFile: File){
        val inputFileStream = FileInputStream(inputFile)
        val inputBuffered = BufferedInputStream(inputFileStream)
        val output = FileOutputStream(outputFile)
        val inputBzip = BZip2CompressorInputStream(inputBuffered)
        val buffer = ByteArray(bufferSize)
        var position = inputBzip.read(buffer)
        while (position!=-1){
            output.write(buffer,0, position)
            position = inputBzip.read(buffer)
        }
        inputBzip.close()
        output.close()
    }
    fun compressFile(inputFile: File, outputFile: File){
        val outputFileStream = FileOutputStream(outputFile)
        val outputBuffered = BufferedOutputStream(outputFileStream)
        val outputBzip = BZip2CompressorOutputStream(outputBuffered)
        val inputFileStream = FileInputStream(inputFile)
        val buffer = ByteArray(bufferSize)
        var position = inputFileStream.read(buffer)
        while (position!=-1){
            outputBzip.write(buffer,0, position)
            position = inputFileStream.read(buffer)
        }
        inputFileStream.close()
        outputBzip.close()
    }

}