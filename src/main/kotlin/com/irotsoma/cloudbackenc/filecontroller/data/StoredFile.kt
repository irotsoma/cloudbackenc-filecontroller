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
 * Created by irotsoma on 6/7/17.
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
@Table(name="stored_file")
class StoredFile(@Id @Column(name="uuid", unique = true, nullable = false, updatable = false)val uuid: UUID,
                 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="watched_location_uuid", unique = false, nullable = false, updatable = false)val watchedLocation: WatchedLocation,
                 @Column(name="path", nullable=false, updatable = false) val path: String,
                 @Column(name="last_updated", nullable=false) var lastUpdated: Date){
    @OneToMany(mappedBy = "storedFileUuid", fetch = FetchType.LAZY)
    var storedFileVersions: Set<StoredFileVersion>? = hashSetOf()
    @Column(name="watched_location_uuid", unique = false, nullable = false, updatable = false,insertable = false)
    lateinit var watchedLocationUuid:UUID
}