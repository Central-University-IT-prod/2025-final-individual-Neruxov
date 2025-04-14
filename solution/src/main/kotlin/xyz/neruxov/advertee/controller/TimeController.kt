package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.neruxov.advertee.data.time.dto.TimeDto
import xyz.neruxov.advertee.service.TimeService

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/time")
class TimeController(private val timeService: TimeService) {

    @PostMapping("/advance")
    fun advanceTime(@RequestBody @Valid time: TimeDto): TimeDto = timeService.setCurrentDate(time.currentDate)

}