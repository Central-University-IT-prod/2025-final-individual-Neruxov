package xyz.neruxov.advertee

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@EnableCaching
@SpringBootApplication
class AdverteeApplication

fun main(args: Array<String>) {
    run(args)
}

fun run(
    args: Array<String>,
    init: SpringApplication.() -> Unit = {},
): ConfigurableApplicationContext {
    return runApplication<AdverteeApplication>(*args, init = init)
}