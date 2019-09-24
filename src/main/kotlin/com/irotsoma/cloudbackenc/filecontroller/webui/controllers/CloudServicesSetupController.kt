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

import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceAuthenticationRequest
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceAuthenticationState
import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceExtensionList
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.SessionConfiguration
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

@Controller
@RequestMapping("/cloudservicessetup")
class CloudServicesSetupController{
    /** kotlin-logging implementation*/
    private companion object: KLogging()
    private val locale: Locale = LocaleContextHolder.getLocale()
    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource
    @Autowired
    private lateinit var sessionConfiguration: SessionConfiguration

    @GetMapping
    fun get(model: Model, session: HttpSession): String {
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return "redirect:/login"
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity<Any>(requestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val cloudServicesListResponse =
                try{
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}", HttpMethod.GET, httpEntity, CloudServiceExtensionList::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true){
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
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
        val cloudServicesUserListResponse =
                try{
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}/user", HttpMethod.GET, httpEntity, CloudServiceExtensionList::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true){
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
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
        val disabledCloudServicesList = cloudServicesListResponse.body
        val enabledCloudServicesList = cloudServicesUserListResponse.body
        enabledCloudServicesList?.forEach { disabledCloudServicesList?.remove(it) }

        addStaticAttributes(model)
        model.addAttribute("disabledExtensions", disabledCloudServicesList)
        model.addAttribute("enabledExtensions", enabledCloudServicesList)

        return "cloudservicessetup"
    }

    @PostMapping(params = ["add"])
    fun addCloudService(@RequestBody formData: MultiValueMap<String, String>, response: HttpServletResponse, model: Model, session: HttpSession): String{
        val selectedItem = formData["disabled-selected-item"]?.get(0)
        if (selectedItem.isNullOrBlank()){
            addStaticAttributes(model)
            model.addAttribute("disabledExtensions", formData["disabledExtensions"])
            model.addAttribute("enabledExtensions", formData["enabledExtensions"])
            model.addAttribute("formError", messageSource.getMessage("cloudServicesSetupController.add.noSelection.error.message", null, locale))
            return "cloudservicessetup"
        }
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return "redirect:/login"
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity<Any>(requestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val cloudServicesListResponse =
                try{
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}/$selectedItem", HttpMethod.GET, httpEntity, CloudServiceExtensionList::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true){
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
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
        val selectedExtension = cloudServicesListResponse.body?.filter { it.extensionUuid == selectedItem }?.get(0)
        if (selectedExtension?.requiresPassword == true || selectedExtension?.requiresUsername == true){
            TODO("handle case by popup prompting for username and/or password")
        }




        addStaticAttributes(model)
        return "cloudservicessetup"
    }

    @PostMapping(params = ["remove"])
    fun removeCloudService(@RequestBody formData: MultiValueMap<String, String>, model: Model, session: HttpSession): String {
        val selectedItem = formData["enabled-selected-item"]?.get(0)
        if (selectedItem.isNullOrBlank()) {
            addStaticAttributes(model)
            model.addAttribute("disabledExtensions", formData["disabledExtensions"])
            model.addAttribute("enabledExtensions", formData["enabledExtensions"])
            model.addAttribute("formError", messageSource.getMessage("cloudServicesSetupController.remove.noSelection.error.message", null, locale))
            return "cloudservicessetup"
        }
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return "redirect:/login"
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity(CloudServiceAuthenticationRequest(),requestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val cloudServiceLogoutResponse =
                try {
                    RestTemplate().exchange("${if (centralControllerSettings.useSSL) {
                        "https"
                    } else {
                        "http"
                    }}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}/logout/$selectedItem", HttpMethod.POST, httpEntity, CloudServiceAuthenticationState::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true) {
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                        if (logger.isDebugEnabled) {
                            errorMessage += "<br><br>${e.localizedMessage}"
                        }
                        model.addAttribute("error", errorMessage)
                        "error"
                    } else {
                        model.addAttribute("error", e.localizedMessage)
                        "error"
                    }
                } catch (e: Exception) {
                    model.addAttribute("error", e.localizedMessage)
                    return "error"
                }

        addStaticAttributes(model)
        return "cloudservicessetup"
    }


    fun addStaticAttributes(model: Model){
        model.addAttribute("pageTitle", messageSource.getMessage("setupCloudServices.label", null, locale))
        model.addAttribute("usernameLabel", messageSource.getMessage("username.label", null, locale))
        model.addAttribute("passwordLabel", messageSource.getMessage("password.label", null, locale))
        model.addAttribute("searchLabel", messageSource.getMessage("search.label", null, locale))
    }
}