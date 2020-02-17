/*
 * Copyright (C) 2016-2020  Irotsoma, LLC
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
package com.irotsoma.cloudbackenc.filecontroller

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Service
import java.io.File
import java.security.KeyStore
import java.security.PublicKey

@Service
class CentralControllerTokenParser {
    @Autowired
    private lateinit var jwtSettings: JwtSettings
    fun getAuthentication(token: String):Authentication{
        val claims = getClaims(token).body
        val roles = claims["roles"] as List<*>? ?: emptyList<String>()
        val authorities = roles.map {
            SimpleGrantedAuthority(it.toString())
        }
        return PreAuthenticatedAuthenticationToken(claims.id, token, authorities)
    }
    fun getClaims(token: String): Jws<Claims>{
        return Jwts.parser()
                .setSigningKey(getPublicKey())
                .parseClaimsJws(token)
    }
    fun getPublicKey(): PublicKey{
        val keyStore: KeyStore
        try {
            keyStore = KeyStore.getInstance(jwtSettings.keyStoreType)
            keyStore?.load(File(jwtSettings.keyStore).inputStream(), jwtSettings.keyStorePassword?.toCharArray())
        } catch (e: Exception) {
            throw Exception("Unable to load JWT keystore.", e)
        }
        return keyStore.getCertificate(jwtSettings.keyAlias).publicKey
    }

}