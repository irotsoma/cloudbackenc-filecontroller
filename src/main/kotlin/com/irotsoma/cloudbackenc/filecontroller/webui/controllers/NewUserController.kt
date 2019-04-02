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

import com.irotsoma.cloudbackenc.common.CloudBackEncRoles
import com.irotsoma.cloudbackenc.filecontroller.CentralControllerSettings
import com.irotsoma.cloudbackenc.filecontroller.webui.models.NewUserForm
import com.irotsoma.cloudbackenc.filecontroller.webui.models.Option
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.*
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@Controller
@RequestMapping("/newuser")
class NewUserController {
    /** kotlin-logging implementation*/
    companion object: KLogging()
    val locale: Locale = LocaleContextHolder.getLocale()

    @Autowired
    lateinit var centralControllerSettings: CentralControllerSettings
    @Autowired
    private lateinit var messageSource: MessageSource
    @GetMapping
    fun get(model: Model): String {
        addStaticAttributes(model)
        model.addAttribute("roles", CloudBackEncRoles.values().map{ Option(it.value, false) })
        return "newuser"
    }
    @PostMapping
    fun createUser(@ModelAttribute @Valid newUserForm: NewUserForm, bindingResult: BindingResult, response: HttpServletResponse, model: Model): String {
        if (bindingResult.hasErrors()) {
            for (error in bindingResult.fieldErrors){
                model.addAttribute("${error.field}Error", error.defaultMessage)
            }
            //Send back previous values for fields
            if (newUserForm.username!=null) {
                model.addAttribute("username", newUserForm.username)
            }
            if (newUserForm.email!=null) {
                model.addAttribute("email", newUserForm.email)
            }
            addStaticAttributes(model)
            model.addAttribute("roles", CloudBackEncRoles.values().map{if (newUserForm.roles.contains(Option(it.value,true))) Option(it.value,true) else Option(it.value,false)})
            return "newuser"
        }


        TODO()
    }
    fun addStaticAttributes(model:Model){
        model.addAttribute("pageTitle", messageSource.getMessage("newUser.label", null, locale))
        model.addAttribute("usernameLabel", messageSource.getMessage("username.label",null,locale))
        model.addAttribute("passwordLabel", messageSource.getMessage("password.label",null,locale))
        model.addAttribute("passwordConfirmLabel", messageSource.getMessage("passwordConfirm.label",null,locale))
        model.addAttribute("emailLabel", messageSource.getMessage("email.label",null,locale))
        model.addAttribute("userRolesLabel", messageSource.getMessage("roles.label",null,locale))
        model.addAttribute("submitButtonLabel", messageSource.getMessage("submit.label",null,locale))

    }
}