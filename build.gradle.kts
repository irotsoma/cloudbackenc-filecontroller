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
import nl.javadude.gradle.plugins.license.DownloadLicensesExtension
import org.jetbrains.dokka.gradle.DokkaTask

group = "com.irotsoma.cloudbackenc"
version = "0.4-SNAPSHOT"

val commonVersion = "0.4-SNAPSHOT"

val kotlinLoggingVersion = "1.8.0.1"
val commonsIoVersion = "1.3.2"
val apacheCommonsCompressionVersion = "1.18"
val webValidationVersion = "1.3"
val jjwtVersion = "0.9.1"
val bootstrapVersion = "4.5.0"
val jqueryVersion = "3.5.1"
val popperVersion = "1.12.9-1"
val openIconicVersion = "1.1.1"
val webjarsLocatorVersion = "0.40"
val javaxValidationVersion = "2.0.1.Final"
val passayVersion = "1.4.0"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")
//try to pull repository credentials from either properties or environment variables
val repoUsername = project.findProperty("ossrhUsername")?.toString() ?: System.getenv("ossrhUsername") ?: ""
val repoPassword = project.findProperty("ossrhPassword")?.toString() ?: System.getenv("ossrhPassword") ?: ""

plugins {
    val springBootVersion = "2.3.1.RELEASE"
    val kotlinVersion = "1.3.72"
    val dokkaVersion = "0.10.1"
    val springDependencyManagementVersion = "1.0.9.RELEASE"
    val liquibaseGradleVersion = "2.0.4"
    id("com.github.hierynomus.license") version "0.15.0"
    java
    signing
    id("io.spring.dependency-management") version springDependencyManagementVersion
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.jetbrains.dokka") version dokkaVersion
    id("org.springframework.boot") version springBootVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.liquibase.gradle") version liquibaseGradleVersion
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
    if (!(project.extra["isReleaseVersion"] as Boolean)) {
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    //spring
    implementation ("org.springframework.boot:spring-boot-starter-web")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation ("org.springframework.boot:spring-boot-starter-log4j2")
    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation ("org.springframework.boot:spring-boot-starter-integration")
    implementation ("org.springframework.boot:spring-boot-starter-mustache")
    implementation ("org.springframework.boot:spring-boot-starter-security")
    kapt ("org.springframework.boot:spring-boot-configuration-processor")
    //jackson
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation ("com.fasterxml.jackson.core:jackson-core")
    implementation ("com.fasterxml.jackson.core:jackson-databind")
    implementation ("com.fasterxml.jackson.core:jackson-annotations")
    //common classes
    implementation ("com.irotsoma.cloudbackenc:common:$commonVersion")
    implementation ("com.irotsoma.cloudbackenc.common:cloudservices:$commonVersion")
    implementation ("com.irotsoma.cloudbackenc.common:encryption:$commonVersion")
    //logging
    implementation ("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    //apache
    implementation ("org.apache.commons:commons-io:$commonsIoVersion")
    implementation ("org.apache.commons:commons-compress:$apacheCommonsCompressionVersion")
    //h2
    implementation ("com.h2database:h2")
    //mariaDb
    implementation ("org.mariadb.jdbc:mariadb-java-client")
    //liquibase
    implementation ("org.liquibase:liquibase-core")
    //jjwt
    implementation ("io.jsonwebtoken:jjwt:$jjwtVersion")
    //javascript
    implementation("com.irotsoma.web:validation:$webValidationVersion")
    implementation("javax.validation:validation-api:$javaxValidationVersion")
    implementation("org.passay:passay:$passayVersion")
    implementation("org.webjars:bootstrap:$bootstrapVersion")
    implementation("org.webjars:jquery:$jqueryVersion")
    implementation("org.webjars:popper.js:$popperVersion")
    implementation("org.webjars:webjars-locator:$webjarsLocatorVersion")
    implementation("org.webjars.bower:open-iconic:$openIconicVersion")
    //test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}
//exclude spring boot logging as it will conflict with kotlin logging
configurations.all{
    exclude(module = "spring-boot-starter-logging")
    exclude(module = "logback-classic")
}
kapt.includeCompileClasspath = false
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    launchScript()
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
if (!(project.extra["isReleaseVersion"] as Boolean)) {
    configurations.all {
        // check for updates every build
        resolutionStrategy {
            cacheDynamicVersionsFor(0, "seconds")
            cacheChangingModulesFor(0, "seconds")
        }
    }
}
signing {
    setRequired({
        (project.extra["isReleaseVersion"] as Boolean)
    })
}
//allow for excluding certain tests via external configuration
tasks.withType<Test>{
    exclude(if (hasProperty("testExclude")) {
                this.property("testExclude").toString()
            } else {
                System.getenv("testExclude") ?: ""
            })
}

//this section downloads some reports regarding the licenses of various dependencies and includes them in the
// META-INF/licenses folder
license {
    ignoreFailures = true
    mapping("kt", "JAVADOC_STYLE")
    mapping("mustache", "XML_STYLE")
    excludes(arrayListOf("**/*.json", "**/*.properties", "**/LICENSE", "**/*license*.html", "**/*license*.xml", "**/COPYING", "**/COPYING.LESSER", "**/*.jar", "**/*.svg"))
}
sourceSets.main{
    resources {
        setSrcDirs(srcDirs.plus(file("$buildDir/license-reports/")))
    }
}
downloadLicenses {
    includeProjectDependencies = true
    dependencyConfiguration = "implementation"
    val apacheTwo = DownloadLicensesExtension.license("The Apache Software License, Version 2.0","http://www.apache.org/licenses/LICENSE-2.0.txt")
    val apache2 = DownloadLicensesExtension.license("Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
    aliases[apacheTwo] = listOf(apache2)
    val test = aliases
}
configurations.implementation{
    isCanBeResolved = true
}
tasks.register<Copy>("copyLicenseReports") {
    from(file("$buildDir/reports/license/"))
    into(file("$buildDir/license-reports/META-INF/licenses"))
    mustRunAfter("downloadLicenses")
}
tasks.assemble{
    dependsOn("downloadLicenses")
    dependsOn("copyLicenseReports")
}
//javadoc stuff for Kotlin
val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/docs/javadoc"
    configuration {
        jdkVersion = 8
        reportUndocumented = true
    }
}
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}
tasks.register<Jar>("javadocJar"){
    dependsOn("dokka")
    archiveClassifier.set("javadoc")
    from(dokka.outputDirectory)
}