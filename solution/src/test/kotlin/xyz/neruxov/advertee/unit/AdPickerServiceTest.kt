package xyz.neruxov.advertee.unit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.mlscore.model.MLScore
import xyz.neruxov.advertee.data.mlscore.repo.MLScoreRepository
import xyz.neruxov.advertee.service.AdPickerService
import xyz.neruxov.advertee.service.TimeService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdAction
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 * проверка алгоритма реализована в E2E тестах, не тут
 */
class AdPickerServiceTest : StringSpec({

    val timeService: TimeService = mockk()
    val adActionRepository: AdActionRepository = mockk()
    val mlScoreRepository: MLScoreRepository = mockk()
    val campaignRepository: CampaignRepository = mockk()

    every { mlScoreRepository.get1stPercentile() } returns 0
    every { mlScoreRepository.get99thPercentile() } returns Int.MAX_VALUE

    every { campaignRepository.getCostPerClick1stPercentile() } returns 0f
    every { campaignRepository.getCostPerClick99thPercentile() } returns 10000f
    every { campaignRepository.getCostPerImpression1stPercentile() } returns 0f
    every { campaignRepository.getCostPerImpression99thPercentile() } returns 10000f

    every { adActionRepository.findAllBy(any()) } returns emptyList()

    val adPickerService = AdPickerService(timeService, adActionRepository, mlScoreRepository, campaignRepository)

    "should return a relevant ad" {
        val client = getRandomClient()
        val campaign = getRandomCampaign(UUID.randomUUID()).copy(targeting = Campaign.Targeting())

        every { campaignRepository.findEligibleCampaigns(any(), any(), any(), any()) } returns listOf(campaign)
        every { timeService.getCurrentDateInt() } returns 1
        every { adActionRepository.findAllByCampaignIdIn(any()) } returns emptyList()
        every { mlScoreRepository.findById(any()) } returns Optional.empty()

        val ad = adPickerService.getRelevantAd(client)

        ad shouldBe campaign
    }

    "should return a relevant ad from multiple campaigns" {
        val client = getRandomClient()
        val campaigns = (1..10).map {
            getRandomCampaign(UUID.randomUUID()).copy(
                targeting = Campaign.Targeting(),
                impressionsLimit = 100
            )
        }

        every { campaignRepository.findEligibleCampaigns(any(), any(), any(), any()) } returns campaigns
        every { timeService.getCurrentDateInt() } returns 1
        every { adActionRepository.findAllByCampaignIdIn(any()) } returns emptyList()
        every { mlScoreRepository.findById(any()) } returns Optional.empty()

        val ad = adPickerService.getRelevantAd(client)

        ad shouldBe campaigns.maxBy { it.costPerImpression }
    }

    "should sort purely by ml score if user has watched all of the ads" {
        val client = getRandomClient()

        val campaigns = (1..10).map {
            getRandomCampaign(UUID.randomUUID()).copy(
                targeting = Campaign.Targeting(),
                impressionsLimit = 100,
                costPerImpression = 10f
            )
        }

        every { campaignRepository.findEligibleCampaigns(any(), any(), any(), any()) } returns campaigns
        every { timeService.getCurrentDateInt() } returns 1
        every { adActionRepository.findAllByCampaignIdIn(any()) } returns campaigns.map {
            getRandomAdAction(
                it.id!!,
                client.id,
                AdAction.Type.IMPRESSION
            )
        }

        every {
            mlScoreRepository.findById(match {
                campaigns.map { campaign -> campaign.advertiserId }.contains(it.advertiserId)
            })
        } answers {
            val id = firstArg<MLScore.Id>()
            val advertiserId = id.advertiserId

            Optional.of(MLScore(id = firstArg(), score = advertiserId.hashCode()))
        }

        val ad = adPickerService.getRelevantAd(client)

        ad shouldBe campaigns.maxBy { it.advertiserId.hashCode() }
    }

    "should return null if no campaigns are eligible" {
        val client = getRandomClient()

        every { campaignRepository.findEligibleCampaigns(any(), any(), any(), any()) } returns emptyList()

        val ad = adPickerService.getRelevantAd(client)

        ad shouldBe null
    }

})