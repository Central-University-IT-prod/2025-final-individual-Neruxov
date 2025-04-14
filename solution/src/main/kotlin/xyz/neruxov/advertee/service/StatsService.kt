package xyz.neruxov.advertee.service

import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.stats.response.DailyStatsResponse
import xyz.neruxov.advertee.data.stats.response.StatsResponse
import xyz.neruxov.advertee.util.countByType
import java.util.*
import kotlin.math.max

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class StatsService(
    private val adActionRepository: AdActionRepository,
    private val timeService: TimeService,
    private val campaignRepository: CampaignRepository,
    private val advertiserService: AdvertiserService
) {

    fun getCampaignStats(campaignId: UUID): StatsResponse {
        campaignRepository.findById(campaignId)
            .orElseThrow { NotFoundException("Campaign with id $campaignId not found") }

        val actions = adActionRepository.findAllByCampaignId(campaignId)

        return getStatsForActions(actions)
    }

    fun getCampaignStatsDaily(campaignId: UUID): List<DailyStatsResponse> {
        campaignRepository.findById(campaignId)
            .orElseThrow { NotFoundException("Campaign with id $campaignId not found") }

        val actions = adActionRepository.findAllByCampaignId(campaignId)

        return getDailyStatsForActions(actions)
    }

    fun getAdvertiserStats(advertiserId: UUID): StatsResponse {
        advertiserService.getById(advertiserId)

        val campaigns = campaignRepository.findAllByAdvertiserId(advertiserId)
        val actions = adActionRepository.findAllByCampaignIdIn(campaigns.map { it.id!! })

        return getStatsForActions(actions)
    }

    fun getAdvertiserStatsDaily(advertiserId: UUID): List<DailyStatsResponse> {
        advertiserService.getById(advertiserId)

        val campaigns = campaignRepository.findAllByAdvertiserId(advertiserId)
        val actions = adActionRepository.findAllByCampaignIdIn(campaigns.map { it.id!! })

        return getDailyStatsForActions(actions)
    }

    private fun getDailyStatsForActions(actions: List<AdAction>): List<DailyStatsResponse> {
        val currentDate = timeService.getCurrentDateInt()

        val result = mutableListOf<DailyStatsResponse>()
        for (i in 0..currentDate) {
            val actionsForDay = actions.filter { it.date == i }
            val dailyStats = getStatsForActions(actionsForDay).toDailyStatsResponse(date = i)

            result.add(dailyStats)
        }

        return result
    }

    private fun getStatsForActions(actions: List<AdAction>): StatsResponse {
        val impressionsCount = actions.countByType(AdAction.Type.IMPRESSION)
        val clicksCount = actions.countByType(AdAction.Type.CLICK)

        val conversion = clicksCount.toFloat() / max(impressionsCount.toFloat(), 1f) * 100 // в процентах

        val spentImpressions =
            actions.filter { it.id.type == AdAction.Type.IMPRESSION }.sumOf { it.cost.toDouble() }.toFloat()
        val spentClicks = actions.filter { it.id.type == AdAction.Type.CLICK }.sumOf { it.cost.toDouble() }.toFloat()

        val spentTotal = spentImpressions + spentClicks

        return StatsResponse(
            impressionsCount = impressionsCount,
            clicksCount = clicksCount,
            conversion = conversion,
            spentImpressions = spentImpressions,
            spentClicks = spentClicks,
            spentTotal = spentTotal
        )
    }

}