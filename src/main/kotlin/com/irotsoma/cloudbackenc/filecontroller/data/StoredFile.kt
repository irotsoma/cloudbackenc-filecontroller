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

/**
 * Created by irotsoma on 6/7/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.data

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 *
 *
 * @author Justin Zak
 */
@Entity
@Table(name="stored_file")
class StoredFile(@Id @Column(name="uuid", unique = true, nullable = false)var uuid: UUID,
                 @Column(name="watched_location_uuid", nullable = false)var watchedLocationUuid: UUID,
                 @Column(name="path", nullable=false) var path: String,
                 @Column(name="remote_uuid", nullable=false) var remoteUuid: UUID,
                 @Column(name="last_updated", nullable=false) var lastUpdated: Date,
                 @Column(name="encryption_is_symmetric", nullable = false ) var encryptionIsSymmetric: Boolean,
                 @Column(name="secret_key", nullable = false) var secretKey:String,
                 @Column(name="public_key", nullable = true) var publicKey:String?,
                 @Column(name="initialization_vector", nullable = true) var iv: ByteArray?)