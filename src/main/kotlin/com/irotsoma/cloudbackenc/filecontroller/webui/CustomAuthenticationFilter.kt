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
 * Created by irotsoma on 4/19/2019.
 */
package com.irotsoma.cloudbackenc.filecontroller.webui

import com.irotsoma.cloudbackenc.filecontroller.CentralControllerTokenParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.support.SpringBeanAutowiringSupport
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * An authentication filter that looks for a cookie containing a valid central controller token and loads the credentials.
 *
 * @property tokenCookieName Name of cookie to look for.
 *
 * @author Justin Zak
 */
@Component
class CustomAuthenticationFilter: GenericFilterBean() {
    @Value("\${filecontroller.webui.tokenCookieName}")
    private lateinit var tokenCookieName: String
    @Autowired
    private lateinit var centralControllerTokenParser: CentralControllerTokenParser
    /**
     * Implements the authentication filter.
     * Loads the appropriate cookie if found and uses the credentials claimed in that token.
     *
     * @param request The servlet request
     * @param response The servlet response
     * @param chain The chain of filters used for authentication.
     *
     * @author Justin Zak
     */
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val session = (request as HttpServletRequest).session
        if (session.getAttribute("SESSION_AUTHENTICATION") != null){
            SecurityContextHolder.getContext().authentication = session.getAttribute("SESSION_AUTHENTICATION") as Authentication
        } else {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, request.servletContext)
            val cookies = request.cookies ?: emptyArray()
            if (cookies.isNotEmpty()) {
                val token = cookies.find { it.name == tokenCookieName }?.value
                if (token != null) {
                    SecurityContextHolder.getContext().authentication = centralControllerTokenParser.getAuthentication(token)
                }
            }
        }
        chain.doFilter(request,response)
    }
}