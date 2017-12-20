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

package com.irotsoma.cloudbackenc.filecontroller.encryption

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.irotsoma.cloudbackenc.common.encryptionserviceinterface.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import javax.annotation.PostConstruct
import kotlin.collections.HashMap

/**
 * Created by irotsoma on 8/18/2016.
 *
 * Implements the encryption services repository
 */

@Component
class EncryptionServiceRepositoryImplementation : EncryptionServiceRepository(), ApplicationContextAware {
    //inject settings
    @Autowired
    lateinit var encryptionServicesSettingsImplementation: EncryptionServicesSettingsImplementation
    //application context must be set before
    lateinit var _applicationContext : ConfigurableApplicationContext
    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        _applicationContext = applicationContext as ConfigurableApplicationContext? ?: throw EncryptionServiceException("Application context in EncryptionServiceRepository is null.")
    }

    override fun buildClassLoader(jarURLs: HashMap<UUID,URL>):URLClassLoader?{
        return URLClassLoader(jarURLs.values.toTypedArray(), _applicationContext.classLoader)
    }
    @PostConstruct
    fun configure(){
        encryptionServicesSettings = encryptionServicesSettingsImplementation

        loadDynamicServices()
    }
}

