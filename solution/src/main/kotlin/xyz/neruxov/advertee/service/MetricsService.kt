package xyz.neruxov.advertee.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import xyz.neruxov.advertee.data.time.repo.TimeRepository
import xyz.neruxov.advertee.util.countByType
import java.util.*
import kotlin.jvm.optionals.getOrNull


/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class MetricsService(
    private val meterRegistry: MeterRegistry,
    private val campaignRepository: CampaignRepository,
    private val adActionRepository: AdActionRepository,
    private val timeRepository: TimeRepository,
    private val reviewRequestRepository: ReviewRequestRepository
) {

    private val dailyRevenues = mutableMapOf<Triple<AdAction.Type, UUID, Int>, Float>()
    private val dailyCounts = mutableMapOf<Triple<AdAction.Type, UUID, Int>, Int>()

    private var maxDate = 0

    init {
        Gauge.builder("advertee_business_campaigns_count") { campaignRepository.count() }
            .description("Amount of campaigns").register(meterRegistry)

        Gauge.builder("advertee_business_active_campaigns") { campaignRepository.countActive(getCurrentDate()) }
            .description("Amount of active campaigns at the moment").register(meterRegistry)

        Gauge.builder("advertee_business_total_revenue") { adActionRepository.getTotalCost() }
            .description("Total amount of money made").register(meterRegistry)

        Gauge.builder("advertee_review_requests_active") { reviewRequestRepository.countByVerdict(null) }
            .description("Amount of active review requests").register(meterRegistry)
    }

    fun updateMaxDate(newDate: Int) {
        if (newDate <= maxDate) {
            return
        }

        for (i in maxDate + 1..newDate) {
            campaignRepository.findAll().forEach {
                incrementDailyRevenue(it.id!!, it.advertiserId, i, AdAction.Type.IMPRESSION, 0f)
                incrementDailyRevenue(it.id!!, it.advertiserId, i, AdAction.Type.CLICK, 0f)

                incrementDailyCount(it.id!!, it.advertiserId, i, AdAction.Type.IMPRESSION, 0)
                incrementDailyCount(it.id!!, it.advertiserId, i, AdAction.Type.CLICK, 0)
            }
        }

        maxDate = newDate
    }

    fun registerNewAction(action: AdAction, advertiserId: UUID) {
        incrementDailyRevenue(
            campaignId = action.id.campaignId,
            advertiserId = advertiserId,
            action.date,
            action.id.type,
            action.cost
        )
        incrementDailyCount(
            campaignId = action.id.campaignId,
            advertiserId = advertiserId,
            action.date,
            action.id.type,
            1
        )
    }

    private fun incrementDailyRevenue(
        campaignId: UUID,
        advertiserId: UUID,
        date: Int,
        type: AdAction.Type,
        amount: Float
    ) {
        val key = Triple(type, campaignId, date)
        dailyRevenues[key] = dailyRevenues.getOrDefault(key, 0f) + amount

        Gauge.builder("advertee_business_daily_revenue") { dailyRevenues.getOrDefault(key, 0f) }
            .description("Total amount of money made per day")
            .tag("day", date.toString())
            .tag("campaign", campaignId.toString())
            .tag("advertiser", advertiserId.toString())
            .tag("type", type.toString())
            .register(meterRegistry)
    }

    private fun incrementDailyCount(campaignId: UUID, advertiserId: UUID, date: Int, type: AdAction.Type, amount: Int) {
        val key = Triple(type, campaignId, date)
        dailyCounts[key] = dailyCounts.getOrDefault(key, 0) + amount

        Gauge.builder("advertee_business_daily_count") { dailyCounts.getOrDefault(key, 0) }
            .description("Total amount of actions made per day")
            .tag("day", date.toString())
            .tag("campaign", campaignId.toString())
            .tag("advertiser", advertiserId.toString())
            .tag("type", type.toString())
            .register(meterRegistry)
    }

    init {
        val start = System.currentTimeMillis()

        campaignRepository.findAll().forEach {
            val actions = adActionRepository.findAllByCampaignId(it.id!!)
            for (i in 0..getCurrentDate()) {
                val actionsDaily = actions.filter { act -> act.date == i }

                val impressions = actionsDaily.countByType(AdAction.Type.IMPRESSION)
                val clicks = actionsDaily.countByType(AdAction.Type.CLICK)

                val impressionsSpent =
                    actionsDaily.filter { act -> act.id.type == AdAction.Type.IMPRESSION }.map { act -> act.cost }.sum()
                val clicksSpent =
                    actionsDaily.filter { act -> act.id.type == AdAction.Type.CLICK }.map { act -> act.cost }.sum()

                incrementDailyRevenue(it.id!!, it.advertiserId, i, AdAction.Type.IMPRESSION, impressionsSpent)
                incrementDailyRevenue(it.id!!, it.advertiserId, i, AdAction.Type.CLICK, clicksSpent)

                incrementDailyCount(it.id!!, it.advertiserId, i, AdAction.Type.IMPRESSION, impressions)
                incrementDailyCount(it.id!!, it.advertiserId, i, AdAction.Type.CLICK, clicks)
            }
        }

        maxDate = getCurrentDate()
        println("MetricsService init took ${System.currentTimeMillis() - start}ms")
    }

    private fun getCurrentDate(): Int = timeRepository.findById(TIME_DAY_DB_ID).getOrNull()?.date ?: 0

}