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

import com.irotsoma.cloudbackenc.common.AuthenticationToken
import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.common.CloudBackEncUser
import com.irotsoma.cloudbackenc.common.UserAccountState
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.SessionConfiguration
import com.irotsoma.cloudbackenc.filecontroller.data.CentralControllerUser
import com.irotsoma.cloudbackenc.filecontroller.data.CentralControllerUserRepository
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import com.irotsoma.cloudbackenc.filecontroller.webui.models.NewUserForm
import com.irotsoma.cloudbackenc.filecontroller.webui.models.Option
import mu.KLogging
import org.apache.tomcat.util.codec.binary.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.regex.Pattern
import javax.servlet.http.HttpSession
import javax.validation.Valid

@Controller
@Lazy
@RequestMapping("/newuser")
class NewUserController {
    /** kotlin-logging implementation*/
    private companion object: KLogging()
    private val locale: Locale = LocaleContextHolder.getLocale()

    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var centralControllerUserRepository: CentralControllerUserRepository
    @Autowired
    private lateinit var messageSource: MessageSource
    @Autowired
    private lateinit var sessionConfiguration: SessionConfiguration

    @GetMapping
    fun get(model: Model, session: HttpSession): String {
        if (session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) == null) {
            return "redirect:/login"
        }
        addStaticAttributes(model)
        val roles = ArrayList<Option>()
        CloudBackEncRoles.values().forEach{ if (it != CloudBackEncRoles.ROLE_TEST) {roles.add(Option(it.value, false))} }
        model.addAttribute("roles", roles)
        return "newuser"
    }
    @PostMapping
    fun createUser( @Valid newUserForm: NewUserForm, bindingResult: BindingResult, model: Model, session: HttpSession): String {
        val token = session.getAttribute(sessionConfiguration.sessionSecurityTokenAttribute) ?: return "redirect:/login"
        if (bindingResult.hasErrors()) {
            val processedMessages = ArrayList<String>()
            for (error in bindingResult.fieldErrors){
                if (error.field == "password"){
                    if (error.defaultMessage?.contains(Pattern.compile("([A-Z]|_)*:\\{").toRegex()) == true) {
                        for (message in error.defaultMessage!!.split("\u001E")) {
                            val parsedMessage = message.split(":")
                            processedMessages.add("<div data-toggle=\"tooltip\" data-placement=\"top\" title=\"${parsedMessage[1].replace("\"", "&quot;", false)}\">${parsedMessage[0]}</div>")
                            val messageString = processedMessages.joinToString(separator = "")
                            model.addAttribute("passwordError", messageString)
                        }
                    }
                    else {
                        model.addAttribute("${error.field}Error", error.defaultMessage)
                    }
                }
                else {
                    model.addAttribute("${error.field}Error", error.defaultMessage)
                }
            }
            //Send back previous values for fields
            if (newUserForm.username != null) {
                model.addAttribute("username", newUserForm.username)
            }
            if (newUserForm.email!=null) {
                model.addAttribute("email", newUserForm.email)
            }
            if (newUserForm.emailConfirm!=null) {
                model.addAttribute("emailConfirm", newUserForm.emailConfirm)
            }
            addStaticAttributes(model)
            val roles = ArrayList<Option>()
            CloudBackEncRoles.values().forEach{ if (it != CloudBackEncRoles.ROLE_TEST && newUserForm.roles.contains(Option(it.value,true))) {roles.add(Option(it.value, true))} else if (it != CloudBackEncRoles.ROLE_TEST) {roles.add(Option(it.value,false)) }}
            model.addAttribute("roles", roles)
            return "newuser"
        }
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }

        val newUsername = newUserForm.username!!.trim()

        //call post to central controller users
        val requestHeaders = HttpHeaders()
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val httpEntity = HttpEntity(CloudBackEncUser(newUsername, newUserForm.password!!, newUserForm.email,UserAccountState.ACTIVE, newUserForm.roles.map{ CloudBackEncRoles.valueOf(it.name) }), requestHeaders)
        val callResponse =
            try {
                RestTemplate().postForEntity("${if (centralControllerSettings.useSSL) {
                    "https"
                } else {
                    "http"
                }}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.usersPath}", httpEntity, CloudBackEncUser::class.java)
            } catch (e: HttpClientErrorException) {
                return if (e.rawStatusCode == 401) {
                        model.addAttribute("formError",messageSource.getMessage("newUserController.administratorOnly.message", null, locale))
                        //Send back previous values for fields
                        if (newUserForm.username != null) {
                            model.addAttribute("username", newUserForm.username)
                        }
                        if (newUserForm.email!=null) {
                            model.addAttribute("email", newUserForm.email)
                        }
                        if (newUserForm.emailConfirm!=null) {
                            model.addAttribute("emailConfirm", newUserForm.emailConfirm)
                        }
                        addStaticAttributes(model)
                        val roles = ArrayList<Option>()
                        CloudBackEncRoles.values().forEach{ if (it != CloudBackEncRoles.ROLE_TEST && newUserForm.roles.contains(Option(it.value,true))) {roles.add(Option(it.value, true))} else {roles.add(Option(it.value,false)) }}
                        model.addAttribute("roles", roles)
                        "newuser"
                    } else {
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                        if (logger.isDebugEnabled){
                            errorMessage += "<br><br>${e.localizedMessage}"
                        }
                        model.addAttribute("error", errorMessage)
                        "error"
                    }
            } catch(e: Exception) {
                model.addAttribute("status", "")
                var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                if (logger.isDebugEnabled){
                    errorMessage += "<br><br>${e.localizedMessage}"
                }
                model.addAttribute("error", errorMessage)
                return "error"
            }
        logger.debug { "New User call response: ${callResponse.statusCode}: ${callResponse.statusCodeValue}" }

        //request a login token for the new user
        if (callResponse.statusCode == HttpStatus.CREATED) {
            val plainUserCredentials = "$newUsername:${newUserForm.password}".toByteArray()
            val base64UserCredentials = String(Base64.encodeBase64(plainUserCredentials))
            val tokenRequestHeaders = HttpHeaders()
            tokenRequestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic $base64UserCredentials")
            val httpTokenEntity = HttpEntity<Any>(tokenRequestHeaders)
            //make call to create a token
            val tokenResponse =
                    try {
                        RestTemplate().exchange("${if (centralControllerSettings.useSSL) {
                            "https"
                        } else {
                            "http"
                        }}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.authPath}", HttpMethod.GET, httpTokenEntity, AuthenticationToken::class.java)
                    } catch (e: Exception) {
                        model.addAttribute("status", "")
                        var errorMessage = messageSource.getMessage("centralcontroller.error.message", null, locale)
                        if (logger.isDebugEnabled){
                            errorMessage += "<br><br>${e.localizedMessage}"
                        }
                        model.addAttribute("error", errorMessage)
                        return "error"
                    }
            logger.debug { "New User token call response: ${tokenResponse.statusCode}: ${tokenResponse.statusCodeValue}" }
            if (tokenResponse.statusCode == HttpStatus.OK && tokenResponse.body != null) {
                //update or insert user in database
                //Note: sets expiration to 100 years in the future if it's null as a workaround for no expiration date
                val userAccount = centralControllerUserRepository.findByUsername(newUsername)
                if (userAccount == null){
                    CentralControllerUser(newUsername, tokenResponse.body!!.token, tokenResponse.body!!.tokenExpiration)
                } else {
                    userAccount.token = tokenResponse.body!!.token
                    userAccount.tokenExpiration = tokenResponse.body!!.tokenExpiration
                }
            }
        }
        model.addAttribute("pageTitle", messageSource.getMessage("newUserController.redirect.title", null, locale))
        model.addAttribute("message", messageSource.getMessage("newUserController.redirect.message", null, locale))
        model.addAttribute("location", "/")
        return "redirect"
    }
    fun addStaticAttributes(model:Model) {
        model.addAttribute("pageTitle", messageSource.getMessage("newUser.label", null, locale))
        model.addAttribute("usernameLabel", messageSource.getMessage("username.label",null,locale))
        model.addAttribute("passwordLabel", messageSource.getMessage("password.label",null,locale))
        model.addAttribute("passwordConfirmLabel", messageSource.getMessage("passwordConfirm.label",null,locale))
        model.addAttribute("emailLabel", messageSource.getMessage("email.label",null,locale))
        model.addAttribute("emailConfirmLabel", messageSource.getMessage("emailConfirm.label",null,locale))
        model.addAttribute("userRolesLabel", messageSource.getMessage("roles.label",null,locale))
        model.addAttribute("submitButtonLabel", messageSource.getMessage("submit.label",null,locale))
    }
}