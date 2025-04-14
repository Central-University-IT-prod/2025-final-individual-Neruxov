package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.ad.dto.Ad
import xyz.neruxov.advertee.data.ad.request.AdClickRegisterRequest
import xyz.neruxov.advertee.service.AdService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/ads")
class AdController(private val adService: AdService) {

    @GetMapping
    fun getRelevantAd(@RequestParam("client_id") clientId: UUID): Ad =
        adService.getRelevantAd(clientId)

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{campaignId}/click")
    fun registerClick(@PathVariable campaignId: UUID, @RequestBody @Valid body: AdClickRegisterRequest): Unit =
        adService.registerClick(campaignId, body)

}