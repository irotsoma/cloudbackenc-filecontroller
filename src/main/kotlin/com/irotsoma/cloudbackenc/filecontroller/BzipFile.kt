/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
 * Created by irotsoma on 11/14/16.
 */
package com.irotsoma.cloudbackenc.filecontroller

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.*

/**
 * Bzip compression/decompression utility
 *
 * @author Justin Zak
 */
class BzipFile {
    companion object{
        /**
         * file buffer size
         */
        val bufferSize = 2048
    }
    /**
     * decompress a compressed file
     *
     * @param inputFile The compressed file to decompress
     * @param outputFile The file object that will hold the decompressed file
     */
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
    /**
     * compress a file
     *
     * @param inputFile The file to compress
     * @param outputFile The file object that will hold the compressed file
     */
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