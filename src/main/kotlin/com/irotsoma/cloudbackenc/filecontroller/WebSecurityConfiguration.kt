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
 * Created by irotsoma on 8/15/2016.
 */
package com.irotsoma.cloudbackenc.filecontroller

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


/**
 * Security configuration for REST controllers.
 * Security is not necessary for this web application.
 * Security is handled by central controller endpoints.
 *
 * @author Justin Zak
 */
@Configuration
@EnableWebSecurity
class WebSecurityConfiguration : WebSecurityConfigurerAdapter(true) {

    /**
     * Allow access to all endpoints. Security is not necessary for this web application.
     * Security is handled by central controller endpoints.
     *
     * @param http Security configuration object.
     */
    override fun configure(http: HttpSecurity){
        http.csrf().disable().authorizeRequests().anyRequest().permitAll()
    }
}