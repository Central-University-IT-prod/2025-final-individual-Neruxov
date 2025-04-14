package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.service.AdvertiserService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/advertisers")
class AdvertiserController(private val advertiserService: AdvertiserService) {

    @GetMapping("/{id}")
    fun getAdvertiserById(
        @PathVariable id: UUID
    ): Advertiser = advertiserService.getById(id)

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAdvertisers(
        @RequestBody @Valid advertisers: List<Advertiser>
    ): List<Advertiser> = advertisers.map { advertiserService.create(it) }

}