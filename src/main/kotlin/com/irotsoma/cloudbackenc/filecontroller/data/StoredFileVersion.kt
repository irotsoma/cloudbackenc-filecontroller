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
 * Created by irotsoma on 6/8/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.data

import java.util.*
import javax.persistence.*

/**
 * Represents a version of a file that is stored on a cloud service.
 *
 * Contains the encryption information used for the individual version of the file.
 *
 * @author Justin Zak
 */
@Entity
@Table(name="stored_file_version")
class StoredFileVersion(@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="stored_file_uuid", unique = false, nullable = false, updatable = false)val storedFile: StoredFile,
                        @Column(name="remote_file_uuid", nullable=false, updatable = false) val remoteFileUuid: UUID,
                        @Column(name="remote_file_version", nullable=false, updatable = false) val remoteFileVersion: Long,
                        /* moving encryption to central controller
                        @Column(name="initialization_vector", nullable = true, updatable = false) val iv: ByteArray?,
                        @Column(name="encryption_service_uuid", nullable= true, updatable = false)val encryptionUuid: UUID?,
                        @Column(name="encryption_is_symmetric", nullable = false, updatable = false ) val encryptionIsSymmetric: Boolean,
                        @Column(name="encryption_algorithm", nullable=false, updatable = false) val encryptionAlgorithm: String,
                        @Column(name="encryption_key_algorithm", nullable=false, updatable = false) val encryptionKeyAlgorithm: String,
                        @Column(name="encryption_block_size",nullable=false, updatable = false)val encryptionBlockSize: Int,
                        @Column(name="secret_key", nullable = false, updatable = false) val secretKey:String,
                        @Column(name="public_key", nullable = true, updatable = false) val publicKey: String?,
                        */
                        @Column(name="original_hash", nullable=false, updatable = false)val originalHash: String,
                        @Column(name="timestamp", nullable = false, updatable = false) val timestamp:Date/*,
                        @Column(name="encrypted_hash", nullable=false, updatable = false)val encryptedHash: String?*/) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long = -1
    @Column(name="stored_file_uuid", unique = false, nullable = false, updatable = false, insertable = false) lateinit var storedFileUuid:UUID
}