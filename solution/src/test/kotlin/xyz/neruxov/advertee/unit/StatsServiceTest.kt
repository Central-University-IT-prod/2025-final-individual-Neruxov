package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.service.AdvertiserService
import xyz.neruxov.advertee.service.StatsService
import xyz.neruxov.advertee.service.TimeService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdAction
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class StatsServiceTest : StringSpec({

    val adActionRepository: AdActionRepository = mockk()
    val timeService: TimeService = mockk()
    val campaignRepository: CampaignRepository = mockk()
    val advertiserService: AdvertiserService = mockk()

    val statsService = StatsService(adActionRepository, timeService, campaignRepository, advertiserService)

    "should get correct stats" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val actions = (1..10).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.IMPRESSION) } +
                (1..5).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.CLICK) }

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { adActionRepository.findAllByCampaignId(campaign.id!!) } returns actions

        val stats = statsService.getCampaignStats(campaign.id!!)

        stats.impressionsCount shouldBe 10
        stats.clicksCount shouldBe 5
        stats.conversion shouldBe 50f
        stats.spentImpressions shouldBe actions.filter { it.id.type == AdAction.Type.IMPRESSION }.map { it.cost }.sum()
        stats.spentClicks shouldBe actions.filter { it.id.type == AdAction.Type.CLICK }.map { it.cost }.sum()
        stats.spentTotal shouldBe actions.map { it.cost }.sum()
    }

    "should return NotFoundException if not found" {
        val campaignId = UUID.randomUUID()

        every { campaignRepository.findById(campaignId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            statsService.getCampaignStats(campaignId)
        }
    }

    "should get correct daily stats" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val actions = mutableListOf<AdAction>()
        for (i in 1..5) {
            val newActions =
                (1..(2 * i)).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.IMPRESSION) } +
                        (1..i).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.CLICK) }

            actions += newActions.map { it.copy(date = i - 1) }
        }

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { adActionRepository.findAllByCampaignId(campaign.id!!) } returns actions
        every { timeService.getCurrentDateInt() } returns 4

        val stats = statsService.getCampaignStatsDaily(campaign.id!!)

        stats.size shouldBe 5

        for (i in 1..5) {
            val stat = stats[i - 1]
            val dailyActions = actions.filter { it.date == i - 1 }

            stat.date shouldBe i - 1
            stat.impressionsCount shouldBe 2 * i
            stat.clicksCount shouldBe i
            stat.conversion shouldBe 50f
            stat.spentImpressions shouldBe dailyActions.filter { it.id.type == AdAction.Type.IMPRESSION }
                .map { it.cost }.sum()
            stat.spentClicks shouldBe dailyActions.filter { it.id.type == AdAction.Type.CLICK }.map { it.cost }.sum()
            stat.spentTotal shouldBe dailyActions.map { it.cost }.sum()
        }
    }

    "should throw NotFoundException if daily stats not found" {
        val campaignId = UUID.randomUUID()

        every { campaignRepository.findById(campaignId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            statsService.getCampaignStatsDaily(campaignId)
        }
    }

    "should get correct stats by advertiser" {
        val advertiser = getRandomAdvertiser()
        val campaigns = (1..5).map { getRandomCampaign(advertiser.id) }

        val actions = campaigns.flatMap { campaign ->
            (1..10).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.IMPRESSION) } +
                    (1..5).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.CLICK) }
        }

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findAllByAdvertiserId(advertiser.id) } returns campaigns
        every { adActionRepository.findAllByCampaignIdIn(campaigns.map { it.id!! }) } returns actions

        val stats = statsService.getAdvertiserStats(advertiser.id)

        stats.impressionsCount shouldBe 50
        stats.clicksCount shouldBe 25
        stats.conversion shouldBe 50f
        stats.spentImpressions shouldBe actions.filter { it.id.type == AdAction.Type.IMPRESSION }.map { it.cost }.sum()
        stats.spentClicks shouldBe actions.filter { it.id.type == AdAction.Type.CLICK }.map { it.cost }.sum()
        stats.spentTotal shouldBe actions.map { it.cost }.sum()
    }

    "should get correct daily stats by advertiser" {
        val advertiser = getRandomAdvertiser()
        val campaigns = (1..5).map { getRandomCampaign(advertiser.id) }

        val actions = campaigns.flatMap { campaign ->
            val actions = mutableListOf<AdAction>()
            for (i in 1..5) {
                val newActions =
                    (1..(2 * i)).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.IMPRESSION) } +
                            (1..i).map { getRandomAdAction(campaign.id!!, UUID.randomUUID(), AdAction.Type.CLICK) }

                actions += newActions.map { it.copy(date = i - 1) }
            }

            actions
        }

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findAllByAdvertiserId(advertiser.id) } returns campaigns
        every { adActionRepository.findAllByCampaignIdIn(campaigns.map { it.id!! }) } returns actions
        every { timeService.getCurrentDateInt() } returns 4

        val stats = statsService.getAdvertiserStatsDaily(advertiser.id)

        stats.size shouldBe 5

        for (i in 1..5) {
            val stat = stats[i - 1]
            val dailyActions = actions.filter { it.date == i - 1 }

            stat.date shouldBe i - 1
            stat.impressionsCount shouldBe 2 * i * campaigns.size
            stat.clicksCount shouldBe i * campaigns.size
            stat.conversion shouldBe 50f
            stat.spentImpressions shouldBe dailyActions.filter { it.id.type == AdAction.Type.IMPRESSION }
                .map { it.cost }.sum()
            stat.spentClicks shouldBe dailyActions.filter { it.id.type == AdAction.Type.CLICK }.map { it.cost }.sum()
            stat.spentTotal shouldBe dailyActions.map { it.cost }.sum()
        }
    }

})