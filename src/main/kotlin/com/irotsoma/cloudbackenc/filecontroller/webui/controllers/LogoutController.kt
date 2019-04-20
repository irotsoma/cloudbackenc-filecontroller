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
package com.irotsoma.cloudbackenc.filecontroller.webui.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

@Lazy
@Controller
@RequestMapping("/logout")
class LogoutController {
    @Value("\${filecontroller.webui.tokenCookieName}")
    private lateinit var tokenCookieName: String
    @GetMapping
    fun get(response: HttpServletResponse, session:HttpSession): String {
        val cookie = Cookie(tokenCookieName, null)
        cookie.maxAge = 0
        response.addCookie(cookie)
        session.invalidate()
        return "redirect:/login"
    }
}