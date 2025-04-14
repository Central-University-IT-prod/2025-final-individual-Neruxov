package xyz.neruxov.advertee.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.neruxov.advertee.data.stats.response.DailyStatsResponse
import xyz.neruxov.advertee.data.stats.response.StatsResponse
import xyz.neruxov.advertee.service.StatsService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/stats")
class StatController(private val statsService: StatsService) {

    @GetMapping("/campaigns/{campaignId}")
    fun getCampaignStats(@PathVariable campaignId: UUID): StatsResponse = statsService.getCampaignStats(campaignId)

    @GetMapping("/campaigns/{campaignId}/daily")
    fun getCampaignStatsDaily(@PathVariable campaignId: UUID): List<DailyStatsResponse> =
        statsService.getCampaignStatsDaily(campaignId)

    @GetMapping("/advertisers/{advertiserId}/campaigns")
    fun getAdvertiserStats(@PathVariable advertiserId: UUID): StatsResponse =
        statsService.getAdvertiserStats(advertiserId)

    @GetMapping("/advertisers/{advertiserId}/campaigns/daily")
    fun getAdvertiserStatsDaily(@PathVariable advertiserId: UUID): List<DailyStatsResponse> =
        statsService.getAdvertiserStatsDaily(advertiserId)

}