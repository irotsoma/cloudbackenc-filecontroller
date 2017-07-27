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
 * Created by irotsoma on 6/7/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.data

import java.util.*
import javax.persistence.*

/**
 * JPA object representing a user with credentials for logging into the central controller
 *
 * @author Justin Zak
 * @property id Database-generated ID for the user.
 * @property username Username of the central controller user
 * @property token A token to be used for logging in to the central controller
 */
@Entity
@Table(name="central_controller_user")
class CentralControllerUser(@Column(name="username", nullable=false) var username: String,
                            @Column(name="token", nullable=true) var token: String?,
                            @Column(name="token_expiration", nullable=true) var tokenExpiration: Date?){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = -1
}