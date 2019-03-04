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
 * Created by irotsoma on 8/11/17.
 */
package com.irotsoma.cloudbackenc.filecontroller.webui

/**
 *
 *
 * @author Justin Zak
 */
class Menu() {
    var nameProperty: String = ""
    var name: String = ""
    var path: String? = null
    var menuItems:ArrayList<MenuItem> = ArrayList()
    var containsMenuItems = false
    constructor(nameProperty:String, name:String, path:String?, menuItems:ArrayList<MenuItem>): this() {
        this.nameProperty=nameProperty
        this.name = name
        this.path=path
        this.menuItems=menuItems
        if (menuItems.size > 0){
            containsMenuItems = true
        }
    }
}