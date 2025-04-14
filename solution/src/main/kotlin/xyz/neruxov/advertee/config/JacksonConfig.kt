package xyz.neruxov.advertee.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            findAndRegisterModules()
            setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        }
    }

}