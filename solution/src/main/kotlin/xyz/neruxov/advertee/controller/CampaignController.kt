package xyz.neruxov.advertee.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.request.CampaignCreateRequest
import xyz.neruxov.advertee.data.campaign.request.CampaignUpdateRequest
import xyz.neruxov.advertee.service.CampaignService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/advertisers/{advertiserId}/campaigns")
class CampaignController(private val campaignService: CampaignService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCampaign(
        @PathVariable advertiserId: UUID,
        @RequestBody @Valid body: CampaignCreateRequest
    ): Campaign =
        campaignService.create(
            advertiserId, body
        )

    @GetMapping
    fun getCampaigns(
        @PathVariable advertiserId: UUID, @RequestParam @Min(0, message = "Size must be greater or equal to 0") @Max(
            100, message = "Page size must be less than or equal to 100"
        ) size: Int = 20, @RequestParam @Min(0, message = "Page must be greater or equal to 0") page: Int = 0 // с нуля!
    ): List<Campaign> = campaignService.getAllByAdvertiserId(advertiserId, page, size)

    @GetMapping("/{campaignId}")
    fun getCampaign(@PathVariable advertiserId: UUID, @PathVariable campaignId: UUID): Campaign =
        campaignService.getById(advertiserId, campaignId)

    @PutMapping("/{campaignId}")
    fun updateCampaign(
        @PathVariable advertiserId: UUID,
        @PathVariable campaignId: UUID,
        @RequestBody @Valid body: CampaignUpdateRequest
    ): Campaign = campaignService.update(advertiserId, campaignId, body)

    @DeleteMapping("/{campaignId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCampaign(@PathVariable advertiserId: UUID, @PathVariable campaignId: UUID): Unit = campaignService.delete(
        advertiserId = advertiserId, id = campaignId
    )

}