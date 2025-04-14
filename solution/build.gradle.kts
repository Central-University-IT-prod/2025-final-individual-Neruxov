plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.jpa)
    alias(libs.plugins.spring.kotlin)
    alias(libs.plugins.jacoco)
    alias(libs.plugins.ksp)
    alias(libs.plugins.telegram.bot)
}

group = "xyz.neruxov"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    // источник: https://github.com/wpanas/testcontainers-examples/blob/master/spring-kotest/build.gradle.kts

    implementation(platform(libs.testcontainers.bom))
    implementation(libs.bundles.kotlin)

    implementation(libs.caffeine)

    implementation(platform(libs.awspring.bom))
    implementation(libs.awspring.s3)

    implementation(libs.micrometer.prometheus)

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.cache)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.boot.web)
    implementation(libs.spring.ai.openai)

    implementation(libs.springdoc.swagger)

    implementation(libs.telegram.spring.starter)
    ksp(libs.telegram.ksp)

//    developmentOnly(libs.spring.boot.devtools) !!! NOT SUPPORTED BY TELEGRAM BOT LIBRARY
    developmentOnly(libs.spring.boot.docker)

    runtimeOnly(libs.postgres)

    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.localstack)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)

    testImplementation(libs.stove.spring)
    testImplementation(libs.stove.http)
    testImplementation(libs.mockk)

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.kotest)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.outputLocation = file("jacoco")
    }

    doLast {
        println("Test Coverage Report: file://${rootDir}/jacoco/index.html")
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("eu/vendeli/tgbot/generated/*")
                exclude("xyz/neruxov/advertee/tgbot/*")
            }
        })
    )
}

jacoco {
    toolVersion = "0.8.12"
    reportsDirectory = layout.projectDirectory.dir("jacoco")
}