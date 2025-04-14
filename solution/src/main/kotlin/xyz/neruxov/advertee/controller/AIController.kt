package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.neruxov.advertee.data.generation.model.AdContentGenerationResult
import xyz.neruxov.advertee.data.generation.request.AdContentGenerationRequest
import xyz.neruxov.advertee.service.AdGenerationService

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/ai")
class AIController(private val adGenerationService: AdGenerationService) {

    @PostMapping("/ad-content")
    fun generateAdContent(@RequestBody @Valid body: AdContentGenerationRequest): AdContentGenerationResult =
        adGenerationService.generateAdContent(body)

}