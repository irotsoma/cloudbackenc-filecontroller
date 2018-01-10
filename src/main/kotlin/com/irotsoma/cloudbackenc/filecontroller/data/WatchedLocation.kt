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
 * Created by irotsoma on 4/27/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.data

import java.util.*
import javax.persistence.*

/**
 * Represents a location on a drive that is being watched for changes.
 *
 * Contains the encryption information to be used for new versions of the file.
 *
 * @author Justin Zak
 */
@Entity
@Table(name="watched_location")
class WatchedLocation(@Id @Column(name="uuid", unique = true, nullable = false)var uuid: UUID,
                      @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true) var user: CentralControllerUser,
                      @Column(name="path",nullable = false) var path: String,
                      @Column(name="recursive",nullable = true)var recursive: Boolean?,
                      @Column(name="filter", nullable = true) var filter: String?
//                      @Column(name="encryption_service_uuid", nullable= true)var encryptionUuid: UUID?,
//                      @Column(name="encryption_algorithm", nullable=false) var encryptionAlgorithm: String,
//                      @Column(name="encryption_block_size",nullable=false)var encryptionBlockSize: Int,
//                      @Column(name="encryption_key_algorithm", nullable=false) var encryptionKeyAlgorithm: String,
//                      @Column(name="encryption_is_symmetric", nullable = false ) var encryptionIsSymmetric: Boolean,
//                      @Column(name="secret_key", nullable = false) var secretKey:String,
//                      @Column(name="public_key", nullable = true) var publicKey:String?
                      )