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

/*
 * Created by irotsoma on 5/6/2019.
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*
import javax.servlet.http.HttpSession

@Controller
@RequestMapping("/cloudserviceslist")
class CloudServicesListController {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    private val locale: Locale = LocaleContextHolder.getLocale()
    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource

    @GetMapping
    fun get(model: Model, session: HttpSession): String {

        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val cloudServicesListResponse =
            try{
                RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.cloudServicesPath}", HttpMethod.GET, null, CloudServiceExtensionList::class.java)
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

        model.addAttribute("pageTitle", messageSource.getMessage("cloudServices.label", null, locale))
        model.addAttribute("extensionNameLabel", messageSource.getMessage("name.label",null,locale))
        model.addAttribute("extensionVersionLabel", messageSource.getMessage("version.label",null,locale))
        model.addAttribute("extensionUuidLabel", messageSource.getMessage("uuid.label",null,locale))

        model.addAttribute("extensions", cloudServicesListResponse.body)

        return "cloudserviceslist"
    }
}