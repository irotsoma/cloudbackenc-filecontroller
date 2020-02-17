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
 * Created by irotsoma on 5/12/17.
 */
package com.irotsoma.cloudbackenc.filecontroller

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Settings for connecting to the central controller.
 *
 * @author Justin Zak
 */
@Configuration
@ConfigurationProperties(prefix="centralcontroller")
class CentralControllerSettings {
    /**
     * Central Controller REST port number
     */
    var port: Int = 0
    /**
     * Central Controller REST host name
     */
    var host: String = ""
    /**
     * Is Central Controller using SSL?
     */
    var useSSL: Boolean = false
    /**
     * Disables SSL certificate validation.  Should only be true for testing purposes with self signed certificates.
     */
    var disableCertificateValidation: Boolean = false
    /**
     * Path to the cloud services rest service
     *
     * Note: A leading slash will be added if not present
     */
    var cloudServicesPath: String = ""
        set(value){
            field = if (!value.startsWith('/')){
                "/$value"
            } else {
                value
            }
        }
    /**
     * Path to the users rest service
     *
     * Note: A leading slash will be added if not present
     */
    var usersPath: String = ""
        set(value){
            field = if (!value.startsWith('/')){
                "/$value"
            } else {
                value
            }
        }
    /**
     * Path to the auth rest service
     *
     * Note: A leading slash will be added if not present
     */
    var authPath: String = ""
        set(value){
            field = if (!value.startsWith('/')){
                "/$value"
            } else {
                value
            }
        }
    /**
     * Path to the files rest service
     *
     * Note: A leading slash will be added if not present
     */
    var filesPath: String = ""
        set(value){
            field = if (!value.startsWith('/')){
                "/$value"
            } else {
                value
            }
        }
}