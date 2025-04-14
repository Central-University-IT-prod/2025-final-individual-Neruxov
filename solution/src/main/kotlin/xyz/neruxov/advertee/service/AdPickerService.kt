package xyz.neruxov.advertee.service

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.data.mlscore.model.MLScore
import xyz.neruxov.advertee.data.mlscore.repo.MLScoreRepository
import xyz.neruxov.advertee.util.countByType
import xyz.neruxov.advertee.util.enum.GenderFilter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 * тут страх божий, предупреждаю
 */
@Service
class AdPickerService(
    private val timeService: TimeService,
    private val adActionRepository: AdActionRepository,
    private val mlScoreRepository: MLScoreRepository,
    private val campaignRepository: CampaignRepository
) {

    fun getRelevantAd(client: Client): Campaign? {
        var campaigns = campaignRepository.findEligibleCampaigns(
            currentDate = timeService.getCurrentDateInt(),
            clientAge = client.age,
            clientGender = GenderFilter.valueOf(client.gender.name),
            clientLocation = client.location
        )

        if (campaigns.isEmpty()) return null

        val actions = adActionRepository.findAllByCampaignIdIn(campaigns.map { it.id!! })
            .groupBy { campaigns.first { campaign -> campaign.id!! == it.id.campaignId } }

        val filteredActions = campaigns.associateWith { actions[it] ?: listOf() }

        // additional validation!!!
        val initialCampaignSize = campaigns.size
        campaigns = filterCampaigns(campaigns, actionsGetter = { filteredActions[it]!! }, client = client)
        if (campaigns.size != initialCampaignSize) {
            println("Campaigns filtered: ${initialCampaignSize - campaigns.size}")
        }

        if (campaigns.isEmpty()) return null

        val mlScore1stPercentile = mlScoreRepository.get1stPercentile()?.toFloat()
        val mlScore99thPercentile = mlScoreRepository.get99thPercentile()?.toFloat()

        val filteredMLScores = getMLScores(campaigns, client)
        val normalizedMLScores = filteredMLScores.mapValues { it.value.score }
            .normalize(min = mlScore1stPercentile, max = mlScore99thPercentile)
//            .normalize()
//        println("NORMALIZED ML SCORES ${normalizedMLScores.values}")

        val costPerImpression1stPercentile = campaignRepository.getCostPerImpression1stPercentile()
        val costPerClick1stPercentile = campaignRepository.getCostPerClick1stPercentile()

        val costPerClick99thPercentile = campaignRepository.getCostPerClick99thPercentile()
        val costPerImpression99thPercentile = campaignRepository.getCostPerImpression99thPercentile()

        val randomActions = adActionRepository.findAllBy(Limit.of(250))
        var randomActionsConversion = randomActions.countByType(AdAction.Type.CLICK)
            .toFloat() / max(randomActions.countByType(AdAction.Type.IMPRESSION).toFloat(), 1f)

        if (randomActionsConversion == 0f) {
            randomActionsConversion = 0.05f
        }

//        println(randomActionsConversion)

        val cost1thPercentile =
            if (costPerImpression1stPercentile == null || costPerClick1stPercentile == null) null else costPerImpression1stPercentile + costPerClick1stPercentile * randomActionsConversion
        val cost99thPercentile =
            if (costPerImpression99thPercentile == null || costPerClick99thPercentile == null) null else costPerImpression99thPercentile + costPerClick99thPercentile * randomActionsConversion

//        println(cost1thPercentile.toString() + " " + cost99thPercentile)

        val predictedProfits = campaigns.associateWith { campaign ->
            calculatePredictedProfitForImpression(campaign, filteredActions[campaign]!!, randomActionsConversion)
        }

//        println("PREDICTED PROFITS ${predictedProfits.values}")

        val normalizedProfits = predictedProfits.normalize(min = cost1thPercentile, max = cost99thPercentile)
//        val normalizedProfits = predictedProfits.normalize()
//        println("NORMALIZED PROFITS ${normalizedProfits.values}")

        val fulfillmentFactors = campaigns.associateWith { campaign ->
            val impressionsFactor =
                calculateFulfillmentFactor(campaign, filteredActions[campaign]!!, AdAction.Type.IMPRESSION)
            val clicksFactor = calculateFulfillmentFactor(campaign, filteredActions[campaign]!!, AdAction.Type.CLICK)

            return@associateWith impressionsFactor + clicksFactor
//            return@associateWith 1 / (1 + (impressionsFactor + clicksFactor) / 2)
        }

        val normalizedFulfillment = fulfillmentFactors.normalize().map { it.key to 1 - it.value }.toMap()
//        println("NORMALIZED FULFILLMENT ${normalizedFulfillment.values}")

        val scoredCampaigns = campaigns.associateWith { campaign ->
            if (filteredActions[campaign]!!.any { action -> action.id.clientId == client.id && action.id.type == AdAction.Type.IMPRESSION }) // повторный показ денег НЕ приносит, и не считается за показ, поэтому остается только релевантность
                return@associateWith 0.25 * normalizedMLScores[campaign]!!

            return@associateWith 0.5 * normalizedProfits[campaign]!! + 0.25 * normalizedMLScores[campaign]!! +
                    0.15 * normalizedFulfillment[campaign]!!
        }

//        println("SCORES ${scoredCampaigns.values}")

        return scoredCampaigns.maxByOrNull { it.value }!!.key
    }

    private fun calculatePredictedProfitForImpression(
        campaign: Campaign,
        actions: List<AdAction>,
        randomActionsConversion: Float
    ): Float {
        val impressionsCount = actions.countByType(AdAction.Type.IMPRESSION)
        val clicksCount = actions.countByType(AdAction.Type.CLICK)

        val conversion = if (impressionsCount > 50) {
            clicksCount.toFloat() / max(impressionsCount.toFloat(), 1f)
        } else { // доверяем конверсии после 50 показов, иначе считаем на рандомной выборке (из 250 действий)
            randomActionsConversion
        }

        return campaign.costPerImpression + conversion * campaign.costPerClick
    }

    private fun getMLScores(campaigns: List<Campaign>, client: Client): Map<Campaign, MLScore> {
        return campaigns.associateWith { campaign ->
            val advertiserId = campaign.advertiserId
            val clientId = client.id

            val id = MLScore.Id(
                advertiserId = advertiserId,
                clientId = clientId
            )

            mlScoreRepository.findById(id).orElse(
                MLScore(id = id, score = 0)
            )
        }
    }

    private fun calculateFulfillmentFactor(campaign: Campaign, actions: List<AdAction>, type: AdAction.Type): Float {
        val currentDay = timeService.getCurrentDateInt()

        val typeActions = actions.countByType(type)
        val typeActionsGoal = campaign.impressionsLimit

        val campaignTimeProgress = (currentDay - campaign.startDate) / max(campaign.endDate - campaign.startDate, 1)

        val actualTypeActionProgress = typeActions.toFloat() / max(typeActionsGoal.toFloat(), 1f)
        val expectedTypeActionProgress = campaignTimeProgress

        return abs(actualTypeActionProgress - expectedTypeActionProgress) / max(expectedTypeActionProgress, 1)
    }

    private fun <K, V : Number> Map<K, V>.normalize(min: Float? = null, max: Float? = null): Map<K, Float> {
        if (values.toSet().size == 1)
            return mapValues { 0f }

        val floatValues = mapValues { it.value.toFloat() }

        val minValue = min ?: floatValues.values.minOrNull() ?: 0f
        val maxValue = max ?: floatValues.values.maxOrNull() ?: 1f

        return floatValues.mapValues { min(max((it.value - minValue) / (maxValue - minValue), 0f), 1f) }
    }

    private fun <V : Number> List<V>.percentile(percentile: Float): Float {
        val sorted = sortedBy { it.toFloat() }
        val index = (size * percentile).toInt()
        return sorted[index].toFloat()
    }

    private fun isUnderLimit(campaign: Campaign, actions: List<AdAction>): Boolean {
        if (campaign.impressionsLimit == 0) return false

        val impressionsCount = actions.countByType(AdAction.Type.IMPRESSION)
        val progressAfter = (impressionsCount + 1) / campaign.impressionsLimit

        return progressAfter < 1.05
    }

    private fun isCampaignActive(campaign: Campaign): Boolean {
        val currentDate = timeService.getCurrentDateInt()
        return campaign.startDate <= currentDate && campaign.endDate >= currentDate
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun isATarget(client: Client, targeting: Campaign.Targeting): Boolean =
        targeting == null ||
                (targeting.gender == null || targeting.gender!!.matches(client.gender)) &&
                (targeting.ageFrom == null || targeting.ageFrom!! <= client.age) &&
                (targeting.ageTo == null || targeting.ageTo!! >= client.age) &&
                (targeting.location == null || targeting.location!! == client.location)

    private fun filterCampaigns(
        campaigns: List<Campaign>,
        actionsGetter: (Campaign) -> (List<AdAction>),
        client: Client
    ): List<Campaign> {
        return campaigns.filter { campaign ->
            isCampaignActive(campaign) && isATarget(client, campaign.targeting) && isUnderLimit(
                campaign,
                actionsGetter(campaign)
            )
        }
    }

}