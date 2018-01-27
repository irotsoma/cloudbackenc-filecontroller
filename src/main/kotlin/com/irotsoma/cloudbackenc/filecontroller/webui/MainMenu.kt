/*
 * Copyright (C) 2016-2018  Irotsoma, LLC
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
import org.springframework.boot.context.properties.ConfigurationProperties
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
@ConfigurationProperties(prefix="filecontroller.webui.menus")
class MainMenu {

    @Autowired
    private lateinit var messageSource: MessageSource

    val menuLayout = ArrayList<Menu>()

    val menus = ArrayList<Menu>()

    @PostConstruct
    private fun populateValues(){
        val locale = LocaleContextHolder.getLocale()
        //translate properties into a localized names
        for (menu in menuLayout){
            val menuItemsHolder = ArrayList<MenuItem>()
            menu.menuItems.mapTo(menuItemsHolder) { MenuItem(it.nameProperty, messageSource.getMessage(it.nameProperty, null, locale), it.path) }
            menus.add(Menu(menu.nameProperty, messageSource.getMessage(menu.nameProperty, null, locale), menu.path, menuItemsHolder))
        }
    }
}