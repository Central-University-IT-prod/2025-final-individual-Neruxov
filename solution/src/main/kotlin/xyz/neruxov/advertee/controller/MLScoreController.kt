package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.neruxov.advertee.data.mlscore.request.MLScoreRequest
import xyz.neruxov.advertee.service.MLScoreService

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/ml-scores")
class MLScoreController(
    val mlScoreService: MLScoreService
) {

    @PostMapping
    fun putMLScore(@RequestBody @Valid mlScore: MLScoreRequest): Unit = mlScoreService.put(mlScore)

}