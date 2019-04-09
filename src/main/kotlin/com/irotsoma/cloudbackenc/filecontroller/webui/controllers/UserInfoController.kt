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

package com.irotsoma.cloudbackenc.filecontroller.webui.controllers

import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@Controller
@Lazy
@RequestMapping("/userinfo")
class UserInfoController {
    /** kotlin-logging implementation*/
    companion object: KLogging()

    @Autowired
    lateinit var centralControllerSettings: CentralControllerSettings

    @GetMapping
    fun get(model: Model,@CookieValue(name="centralcontroller-token", required=false) token: String?): String {
        if (token == null){
            return "redirect:/login"
        }
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity<Any>(requestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val userResponse =
            try{
                //val testPath = "${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.usersPath}"
                RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.usersPath}", HttpMethod.GET, httpEntity, CloudBackEncUser::class.java)
            } catch (e: HttpClientErrorException) {
                return if (e.rawStatusCode == 401) {
                    "redirect:/login"
                } else {
                    model.addAttribute("status", "")
                    var errorMessage = "Error accessing central controller. Make sure it is running and accessible."
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    "error"
                }
            } catch (e: ResourceAccessException) {
                return if (e.cause?.message?.contains("Connection refused", true) == true){
                    model.addAttribute("status", "")
                    var errorMessage = "Error accessing central controller. Make sure it is running and accessible."
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    "error"
                } else {
                    model.addAttribute("error", e.localizedMessage)
                    "error"
                }
            } catch (e:Exception){
                model.addAttribute("error", e.localizedMessage)
                return "error"
            }
        model.addAttribute("username",userResponse.body?.username ?:"")
        model.addAttribute("email",userResponse.body?.email ?:"")
        model.addAttribute("userRoles",userResponse.body?.roles?.map{it.name} ?: emptyList<String>())


        return "userinfo"
    }
}