package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.ad.request.AdClickRegisterRequest
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.ForbiddenException
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.service.*
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdAction
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient
import java.util.*

class AdServiceTest : StringSpec({

    val campaignRepository: CampaignRepository = mockk()
    val timeService: TimeService = mockk()
    val adActionRepository: AdActionRepository = mockk()
    val adPickerService: AdPickerService = mockk()
    val metricsService: MetricsService = mockk()
    val clientService: ClientService = mockk()

    val adService = AdService(
        adActionRepository,
        campaignRepository,
        timeService,
        clientService,
        adPickerService,
        metricsService
    )

    "should return a relevant ad and register impression" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        every { clientService.getById(client.id) } returns client
        every { adPickerService.getRelevantAd(client) } returns campaign
        every { timeService.getCurrentDateInt() } returns 1
        every { adActionRepository.existsById(any()) } returns false
        every { adActionRepository.save(any()) } returns mockk()
        every { metricsService.registerNewAction(any(), any()) } just Runs

        val ad = adService.getRelevantAd(client.id)

        ad shouldBe campaign.toAd()
        verify { adActionRepository.save(any()) }
    }

    "should not register an impression if user has already watched the ad" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val adAction = getRandomAdAction(campaign.id!!, client.id, AdAction.Type.IMPRESSION)

        every { clientService.getById(client.id) } returns client
        every { adPickerService.getRelevantAd(client) } returns campaign
        every { timeService.getCurrentDateInt() } returns 1
        every { adActionRepository.existsById(adAction.id) } returns true

        val ad = adService.getRelevantAd(client.id)
        ad shouldBe campaign.toAd()

        verify(exactly = 0) { adActionRepository.save(adAction) }
        verify(exactly = 0) { metricsService.registerNewAction(adAction, advertiser.id) }
    }

    "should throw NotFoundException when no relevant ads found" {
        val client = getRandomClient()

        every { clientService.getById(client.id) } returns client
        every { adPickerService.getRelevantAd(client) } returns null

        shouldThrow<NotFoundException> { adService.getRelevantAd(client.id) }
    }

    "should register a click action if impression exists" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val request = AdClickRegisterRequest(client.id)

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { clientService.getById(client.id) } returns client
        every { adActionRepository.existsById(match { it.type == AdAction.Type.IMPRESSION }) } returns true
        every { adActionRepository.existsById(match { it.type == AdAction.Type.CLICK }) } returns false
        every { adActionRepository.save(any()) } returns mockk()
        every { metricsService.registerNewAction(any(), any()) } just Runs

        adService.registerClick(campaign.id!!, request)

        verify { adActionRepository.save(any()) }
    }

    "should not register a click action if it already exists" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val request = AdClickRegisterRequest(client.id)

        val adAction = getRandomAdAction(campaign.id!!, client.id, AdAction.Type.CLICK)

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { clientService.getById(client.id) } returns client
        every { adActionRepository.existsById(match { it.type == AdAction.Type.IMPRESSION }) } returns true
        every { adActionRepository.existsById(match { it.type == AdAction.Type.CLICK }) } returns true

        adService.registerClick(campaign.id!!, request)

        verify(exactly = 0) { adActionRepository.save(adAction) }
        verify(exactly = 0) { metricsService.registerNewAction(adAction, advertiser.id) }
    }

    "should throw ForbiddenException if clicking on an ad without impression" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        val request = AdClickRegisterRequest(client.id)

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { clientService.getById(client.id) } returns client
        every { adActionRepository.existsById(match { it.type == AdAction.Type.IMPRESSION }) } returns false

        shouldThrow<ForbiddenException> { adService.registerClick(campaign.id!!, request) }
    }

    "should throw NotFoundException if campaign not found" {
        val client = getRandomClient()
        val campaignId = UUID.randomUUID()
        val request = AdClickRegisterRequest(client.id)

        every { campaignRepository.findById(campaignId) } returns Optional.empty()

        shouldThrow<NotFoundException> { adService.registerClick(campaignId, request) }
    }

})
