/*
 * Copyright (C) 2016-2017  Irotsoma, LLC
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
 * Created by irotsoma on 8/11/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.webui

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.PostConstruct


/**
 *
 *
 * @author Justin Zak
 */
@Component
class MainMenu {

    @Autowired
    private lateinit var messageSource: MessageSource

    val menuItems = ArrayList<Menu>()

    @PostConstruct
    private fun populateValues(){
        val locale = LocaleContextHolder.getLocale()
        menuItems.add(Menu(messageSource.getMessage("filecontroller.menuitem.login",null,locale),"/login"))
        menuItems.add(Menu(messageSource.getMessage("filecontroller.menuitem.users",null,locale),"/users"))
        menuItems.add(Menu(messageSource.getMessage("filecontroller.menuitem.setup.cloud.services",null,locale),"/cloud-services"))

    }
}