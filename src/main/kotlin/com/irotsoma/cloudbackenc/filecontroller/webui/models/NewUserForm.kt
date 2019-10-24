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

package com.irotsoma.cloudbackenc.filecontroller.webui.models

import com.irotsoma.web.validation.FieldMatch
import com.irotsoma.web.validation.ValidPassword
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

@FieldMatch.List([
    FieldMatch(first = "password", second = "passwordConfirm", message = "The password fields must match"),
    FieldMatch(first = "email", second = "emailConfirm", message = "The email fields must match")
])
class NewUserForm {
    @NotEmpty
    var username: String? = null
    @NotEmpty
    @ValidPassword
    var password: String? = null
    @NotEmpty
    var passwordConfirm: String? = null
    @Email
    var email: String? = null
    @Email
    var emailConfirm: String? = null
    var roles: Array<Option> = emptyArray()
}