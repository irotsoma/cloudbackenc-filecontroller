/*
 * Copyright (C) 2016-2019  Irotsoma, LLC
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
 * Created by irotsoma on 10/20/16.
 */
package com.irotsoma.cloudbackenc.filecontroller.webui.controllers.exceptions

import com.irotsoma.cloudbackenc.common.RestException
import com.irotsoma.cloudbackenc.common.RestExceptionExceptions

/**
 * Custom exception for trying to access /file-encryptors with an invalid UUID
 */
class InvalidEncryptionUUIDException() : RestException(RestExceptionExceptions.INVALID_ENCRYPTION_SERVICE_UUID)

/**
 * Custom exception for when the system can not find the file requested to be encrypted.
 */
class EncryptionFileNotFoundException(): RestException(RestExceptionExceptions.FILE_NOT_FOUND)

/**
 * Custom exception for when the system does not support the requested algorithm.
 */
class UnsupportedEncryptionAlgorithmException(): RestException(RestExceptionExceptions.UNSUPPORTED_ENCRYPTION_ALGORITHM)

/**
 * Custom exception for when the system can not write the decrypted file to the disk.
 */
class FileNotWritableException() : RestException(RestExceptionExceptions.FILE_NOT_WRITABLE)