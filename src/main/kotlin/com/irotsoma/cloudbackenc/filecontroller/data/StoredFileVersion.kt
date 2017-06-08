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
 * Created by irotsoma on 6/8/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.data

import java.util.*
import javax.persistence.*

/**
 *
 *
 * @author Justin Zak
 */
@Entity
@Table(name="stored_file_version")
class StoredFileVersion(@Column(name="stored_file_uuid", unique = true, nullable = false)var storedFileUuid: UUID,
                        @Column(name="remote_file_uuid", nullable=true) var remoteFileUuid: UUID?,
                        @Column(name="initialization_vector", nullable = true) var iv: ByteArray?,
                        @Column(name="encryption_service_uuid", nullable= true)var encryptionServiceUuid: UUID?,
                        @Column(name="encryption_is_symmetric", nullable = false ) var encryptionIsSymmetric: Boolean,
                        @Column(name="encryption_algorithm", nullable=false) var encryptionAlgorithm: String,
                        @Column(name="encryption_key_algorithm", nullable=false) var encryptionKeyAlgorithm: String,
                        @Column(name="encryption_block_size",nullable=false)var encryptionBlockSize: Int,
                        @Column(name="secret_key", nullable = false) var secretKey:String,
                        @Column(name="public_key", nullable = true) var publicKey: String?,
                        @Column(name="timestamp", nullable = false) var timestamp:Date) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1
}