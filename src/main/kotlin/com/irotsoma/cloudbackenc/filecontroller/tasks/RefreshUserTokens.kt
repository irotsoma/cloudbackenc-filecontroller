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
 * Created by irotsoma on 7/7/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.tasks

import com.irotsoma.cloudbackenc.common.AuthenticationToken
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.data.CentralControllerUserRepository
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 *
 *
 * @author Justin Zak
 */
@Component
class RefreshUserTokens {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    @Autowired
    lateinit var centralControllerUserRepository: CentralControllerUserRepository
    @Autowired
    lateinit var centralControllerSettings: CentralControllerSettings

    @Scheduled(fixedDelayString="\${centralcontroller.tokenRefreshInterval}")
    fun doRefresh(){
        val centralControllerProtocol = if (centralControllerSettings.useSSL) "https" else "http"
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "Central Controller SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val centralControllerURL = "$centralControllerProtocol://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.authPath}/token"

        val currentUsers = centralControllerUserRepository.findByTokenNotNull() ?: return
        for (user in currentUsers) {
            val requestHeaders = HttpHeaders()
            requestHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ${user.token}")
            val httpEntity = HttpEntity<Any>(requestHeaders)
            val response : ResponseEntity<AuthenticationToken>? =
            try {
                RestTemplate().exchange(centralControllerURL, HttpMethod.GET, httpEntity, AuthenticationToken::class.java)
            } catch (ignore: Exception) { null }

            //TODO: Check for other errors and remove the token from the DB if it's no longer valid (i.e. received unauthorized error with current token)

            if (response?.body?.token != null){
                user.token = response.body?.token
                centralControllerUserRepository.save(user)
            }


        }
    }
}