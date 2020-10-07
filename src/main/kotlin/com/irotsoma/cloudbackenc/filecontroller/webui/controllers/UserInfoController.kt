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

package com.irotsoma.cloudbackenc.filecontroller.webui.controllers

import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.UserAccountState
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.SessionConfiguration
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import com.irotsoma.cloudbackenc.filecontroller.webui.models.ChangePasswordForm
import com.irotsoma.cloudbackenc.filecontroller.webui.models.FormResponse
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.*
import javax.servlet.http.HttpSession
import javax.validation.Valid

@Controller
@Lazy
@RequestMapping("/userinfo")
class UserInfoController {
    /** kotlin-logging implementation*/
    private companion object: KLogging()
    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource
    @Autowired
    private lateinit var sessionConfiguration: SessionConfiguration

    @GetMapping(value=["/{username}", "/", ""])
    fun get(model: Model, session: HttpSession, @PathVariable(required=false) username: String?): String {
        val locale: Locale = LocaleContextHolder.getLocale()
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return "redirect:/login"
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity<Any>(requestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val usernameString = if (username==null){""} else {"/$username"}
        val userResponse =
            try{
                RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.usersPath}$usernameString", HttpMethod.GET, httpEntity, CloudBackEncUser::class.java)
            } catch (e: HttpClientErrorException) {
                return if (e.rawStatusCode == 401) {
                    "redirect:/login"
                } else {
                    model.addAttribute("status", "")
                    var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                    if (logger.isDebugEnabled) {
                        errorMessage += "<br><br>${e.localizedMessage}"
                    }
                    model.addAttribute("error", errorMessage)
                    "error"
                }
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
        addStaticAttributes(model)
        model.addAttribute("username",userResponse.body?.username ?:"")
        model.addAttribute("email",userResponse.body?.email ?:"")
        model.addAttribute("userRoles",userResponse.body?.roles?.map { it.name } ?: emptyList<String>())
        model.addAttribute("userState", userResponse.body?.state?.name?.toLowerCase()?.capitalize() ?: "")

        return "userinfo"
    }
    /**
     * Used to change a password using an ajax method
     *
     * @param changePasswordForm A validated model of the form containing the record.
     * @param bindingResult Validation results for the form data.
     * @return A FormResponse that contains a boolean parameter "validated" which is true if the add was successful or false if errors, and a map of field name to message for any errors.
     */
    @PostMapping(value=["/{username}/ajax","/ajax"])
    @ResponseBody
    fun post(@Valid changePasswordForm: ChangePasswordForm, bindingResult: BindingResult, @PathVariable(required=false) username: String?, session: HttpSession): FormResponse {
        val locale: Locale = LocaleContextHolder.getLocale()
        val authorizedUser = SecurityContextHolder.getContext().authentication.name
        val requestedUser= if (username.isNullOrBlank()){authorizedUser} else {username}
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return FormResponse("password error", false, mapOf(Pair("userName", messageSource.getMessage("dataAccess.error.message",null,locale))))
        if (bindingResult.hasErrors()) {
            val errors = ParseBindingResultErrors.parseBindingResultErrors(bindingResult, messageSource, locale)
            val parsedErrors = hashMapOf<String,String>()
            errors.forEach { (key, value) -> parsedErrors[key.removeSuffix("Error")] = value }
            return FormResponse("password error", false, parsedErrors)
        }
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        //call put to central controller users
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity(CloudBackEncUser(requestedUser,changePasswordForm.password,null,UserAccountState.ACTIVE, emptyList()), requestHeaders)
        val userResponse =
            try{
                RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.usersPath}", HttpMethod.PUT, httpEntity, CloudBackEncUser::class.java)
            } catch (e:Exception){
                val errorMessage = messageSource.getMessage("dataAccess.error.message", null, locale)
                logger.warn { errorMessage }
                return FormResponse("password error", false, mapOf(Pair("password", errorMessage)))
            }
        return if (userResponse.statusCode == HttpStatus.OK){
            FormResponse("password changed", true, null)
        } else {
            val errorMessage = messageSource.getMessage("dataAccess.error.message", null, locale)
            logger.warn { errorMessage }
            FormResponse("password error", false, mapOf(Pair("password", errorMessage)))
        }
    }
    /**
     * Adds a series of model attributes that are required for all GETs
     *
     * @param model The Model object to add the attributes to.
     */
    fun addStaticAttributes(model: Model) {
        val locale: Locale = LocaleContextHolder.getLocale()
        model.addAttribute("pageTitle", messageSource.getMessage("userDetails.label", null, locale))
        model.addAttribute("usernameLabel", messageSource.getMessage("username.label",null,locale))
        model.addAttribute("passwordLabel", messageSource.getMessage("password.label",null,locale))
        model.addAttribute("changePasswordLabel", messageSource.getMessage("password.change.label",null,locale))
        model.addAttribute("confirmPasswordLabel", messageSource.getMessage("password.confirm.label",null, locale))
        model.addAttribute("submitButtonLabel", messageSource.getMessage("submit.label", null, locale))
        model.addAttribute("cancelLabel", messageSource.getMessage("cancel.button.label", null, locale))
        model.addAttribute("detailsLabel", messageSource.getMessage("details.label", null, locale))
        model.addAttribute("userRolesLabel",messageSource.getMessage("roles.label", null, locale))
        model.addAttribute("passwordChangeSuccessMessage",messageSource.getMessage("password.change.success.message", null, locale))
        model.addAttribute("emailLabel", messageSource.getMessage("email.label",null,locale))
        model.addAttribute("userStateLabel", messageSource.getMessage("user.state.label",null,locale))


    }
}