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

import com.irotsoma.cloudbackenc.common.cloudservices.CloudServiceExtensionList
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
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
    companion object: KLogging()
    private val locale: Locale = LocaleContextHolder.getLocale()
    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource

    @GetMapping
    fun get(model: Model): String {

        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            CloudServicesListController.logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val cloudServicesListResponse =
                try{
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}", HttpMethod.GET, null, CloudServiceExtensionList::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (CloudServicesListController.logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true){
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                        if (CloudServicesListController.logger.isDebugEnabled) {
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
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}/user", HttpMethod.GET, null, CloudServiceExtensionList::class.java)
                } catch (e: HttpClientErrorException) {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (CloudServicesListController.logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    return "error"
                } catch (e: ResourceAccessException) {
                    return if (e.cause?.message?.contains("Connection refused", true) == true){
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                        if (CloudServicesListController.logger.isDebugEnabled) {
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
        enabledCloudServicesList.forEach { disabledCloudServicesList.remove(it) }



        addStaticAttributes(model)
        model.addAttribute("disabledExtensions", disabledCloudServicesList)
        model.addAttribute("enabledExtensions", enabledCloudServicesList)
//        val list = CloudServiceExtensionList()
//        list.add(CloudServiceExtension(UUID.randomUUID().toString(), "testName", 1))
//        list.add(CloudServiceExtension(UUID.randomUUID().toString(), "blah", 1))
//        list.add(CloudServiceExtension(UUID.randomUUID().toString(), "silly", 1))
//        model.addAttribute("disabledExtensions", list)



        return "cloudservicessetup"
    }

    @PostMapping(params = ["add"])
    fun addCloudService(@RequestBody formData: MultiValueMap<String, String>, response: HttpServletResponse, model: Model, session: HttpSession): String{


        addStaticAttributes(model)
        return "cloudservicessetup"
    }

    @PostMapping(params = ["remove"])
    fun removeCloudService(@RequestBody formData: MultiValueMap<String, String>, model: Model): String{



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