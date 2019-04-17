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
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.JwtSettings
import com.irotsoma.cloudbackenc.filecontroller.data.CentralControllerUser
import com.irotsoma.cloudbackenc.filecontroller.data.CentralControllerUserRepository
import com.irotsoma.cloudbackenc.filecontroller.trustSelfSignedSSL
import com.irotsoma.cloudbackenc.filecontroller.webui.models.LogInForm
import io.jsonwebtoken.Jwts
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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.File
import java.security.KeyStore
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@Lazy
@Controller
@RequestMapping("/login")
class LogInController {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    private val locale: Locale = LocaleContextHolder.getLocale()

    @Autowired
    private lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource
    @Autowired
    private lateinit var centralControllerUserRepository: CentralControllerUserRepository
    @Autowired
    private lateinit var jwtSettings: JwtSettings

    @GetMapping
    fun get(model: Model): String {
        addStaticAttributes(model)
        return "login"
    }
    @PostMapping
    fun authenticate(@ModelAttribute @Valid logInForm: LogInForm, bindingResult: BindingResult, response: HttpServletResponse, model: Model): String {
        if (bindingResult.hasErrors()) {
            for (error in bindingResult.fieldErrors){
                model.addAttribute("${error.field}Error", error.defaultMessage)
            }
            if (logInForm.username!=null) {
                model.addAttribute("username", logInForm.username)
            }
            addStaticAttributes(model)
            return "login"
        }
        val plainUserCredentials = "${logInForm.username}:${logInForm.password}".toByteArray()
        val base64UserCredentials = String(Base64.encodeBase64(plainUserCredentials))
        val tokenRequestHeaders = HttpHeaders()
        tokenRequestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic $base64UserCredentials")
        val httpTokenEntity = HttpEntity<Any>(tokenRequestHeaders)
        //for testing use a hostname verifier that doesn't do any verification
        if ((centralControllerSettings.useSSL) && (centralControllerSettings.disableCertificateValidation)) {
            trustSelfSignedSSL()
            logger.warn { "SSL is enabled, but certificate validation is disabled.  This should only be used in test environments!" }
        }
        val tokenResponse =
                try{
                    RestTemplate().exchange("${ if (centralControllerSettings.useSSL){"https"}else{"http"}}://${centralControllerSettings.host}:${centralControllerSettings.port}${centralControllerSettings.authPath}", HttpMethod.GET, httpTokenEntity, AuthenticationToken::class.java)
                } catch (e: HttpClientErrorException) {
                    return if (e.rawStatusCode == 401) {
                        if (logInForm.username!=null) {
                            model.addAttribute("username", logInForm.username)
                        }
                        addStaticAttributes(model)
                        model.addAttribute("formError", messageSource.getMessage("logIn.failed.message", null, locale))
                        "login"
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
        return if (tokenResponse.statusCode == HttpStatus.OK) {

            val cookie = Cookie("centralcontroller-token", tokenResponse.body?.token)
            // set the cookie age to equal the token expiration or default to 24 hrs if no expiration time was returned
            cookie.maxAge = (((tokenResponse.body?.tokenExpiration?.time ?: (Date().time + 86400000L)) - Date().time) / 1000).toInt()
            response.addCookie(cookie)
            val token = tokenResponse.body!!.token
            val userAccount = centralControllerUserRepository.findByUsername(logInForm.username!!)
            if (userAccount == null){
                CentralControllerUser(logInForm.username!!, token, tokenResponse.body!!.tokenExpiration)
            } else {
                userAccount.token = token
                userAccount.tokenExpiration = tokenResponse.body!!.tokenExpiration
            }
            val keyStore: KeyStore
            try {
                keyStore = KeyStore.getInstance(jwtSettings.keyStoreType)
                keyStore?.load(File(jwtSettings.keyStore).inputStream(), jwtSettings.keyStorePassword?.toCharArray())
            } catch (e: Exception){
                throw Exception("Unable to load JWT keystore.", e)
            }
            val cert = keyStore.getCertificate(jwtSettings.keyAlias)
            val roles = Jwts.parser()
                    .setSigningKey(cert.publicKey)
                    .parseClaimsJws(token)
                    .body["roles"] as List<*>? ?: emptyList<String>()
            val authorities = roles.map{
                SimpleGrantedAuthority(it.toString())
            }
            val authToken = PreAuthenticatedAuthenticationToken(logInForm.username!!, token, authorities)
            val sc = SecurityContextHolder.getContext()
            sc.authentication = authToken
            val session = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request.session
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc)

            "index"
        } else {
            if (logInForm.username!=null) {
                model.addAttribute("username", logInForm.username)
                model.addAttribute("formError", messageSource.getMessage("logIn.failed.message", null, locale))
            }
            addStaticAttributes(model)
            "login"
        }
    }
    fun addStaticAttributes(model: Model){
        model.addAttribute("usernameLabel", messageSource.getMessage("username.label",null,locale))
        model.addAttribute("passwordLabel", messageSource.getMessage("password.label",null,locale))
        model.addAttribute("submitButtonLabel", messageSource.getMessage("submit.label",null,locale))
        model.addAttribute("pageTitle", messageSource.getMessage("logIn.label", null, locale))

    }
}