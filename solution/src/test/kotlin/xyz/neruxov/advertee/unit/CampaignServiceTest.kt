package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.Pageable
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.InvalidBodyException
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import xyz.neruxov.advertee.service.*
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaignCreateRequest
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaignUpdateRequest
import java.util.*

class CampaignServiceTest : StringSpec({

    val campaignRepository: CampaignRepository = mockk()
    val timeService: TimeService = mockk()
    val advertiserService: AdvertiserService = mockk()
    val adActionRepository: AdActionRepository = mockk()
    val attachmentService: AttachmentService = mockk()
    val moderationService: ModerationService = mockk()
    val reviewRequestRepository: ReviewRequestRepository = mockk()

    val campaignService = CampaignService(
        campaignRepository,
        timeService,
        advertiserService,
        adActionRepository,
        attachmentService,
        moderationService,
        reviewRequestRepository
    )

    "should create a campaign successfully" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomCampaignCreateRequest()

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { timeService.getCurrentDateInt() } returns 1
        every { campaignRepository.save(any()) } answers { firstArg() }
        every { moderationService.isTextEnabled() } returns false

        val campaign = campaignService.create(advertiser.id, request)

        campaign.adTitle shouldBe request.adTitle
        campaign.advertiserId shouldBe advertiser.id

        verify { campaignRepository.save(any()) }
    }

    "should create a campaign successfully with targeting = null" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomCampaignCreateRequest().copy(
            targeting = null
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { timeService.getCurrentDateInt() } returns 1
        every { campaignRepository.save(any()) } answers { firstArg() }
        every { moderationService.isTextEnabled() } returns false

        val campaign = campaignService.create(advertiser.id, request)

        campaign.adTitle shouldBe request.adTitle
        campaign.advertiserId shouldBe advertiser.id

        verify { campaignRepository.save(any()) }
    }

    "should throw exception when start date is greater than end date" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomCampaignCreateRequest().copy(
            startDate = 5,
            endDate = 1
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser

        shouldThrow<InvalidBodyException> {
            campaignService.create(advertiser.id, request)
        }
    }

    "should throw exception when start date is less than current date" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomCampaignCreateRequest().copy(
            startDate = 0
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { timeService.getCurrentDateInt() } returns 1

        shouldThrow<InvalidBodyException> {
            campaignService.create(advertiser.id, request)
        }
    }

    "should return campaign by id if advertiser matches" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)

        val result = campaignService.getById(advertiser.id, campaign.id!!)

        result shouldBe campaign
    }

    "should throw NotFoundException when advertiser does not match" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(UUID.randomUUID())

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)

        shouldThrow<NotFoundException> {
            campaignService.getById(advertiser.id, campaign.id!!)
        }
    }

    "should throw NotFoundException when campaign does not exist" {
        val advertiser = getRandomAdvertiser()
        val campaignId = UUID.randomUUID()

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaignId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            campaignService.getById(advertiser.id, campaignId)
        }
    }

    "should get paged advertiser's campaigns" {
        val advertiser = getRandomAdvertiser()
        val campaigns = (1..10).map { getRandomCampaign(advertiser.id) }

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findAllByAdvertiserId(advertiser.id, any()) } returns campaigns

        val result = campaignService.getAllByAdvertiserId(advertiser.id, 0, 10)

        result shouldBe campaigns
    }

    "should return limited campaigns" {
        val advertiser = getRandomAdvertiser()
        val campaigns = (1..10).map { getRandomCampaign(advertiser.id) }

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every {
            campaignRepository.findAllByAdvertiserId(
                advertiser.id,
                Pageable.ofSize(5).withPage(0)
            )
        } returns campaigns.subList(0, 5)

        val result = campaignService.getAllByAdvertiserId(advertiser.id, 0, 5)

        result shouldBe campaigns.subList(0, 5)
    }

    "should return empty list when size is 0" {
        val advertiser = getRandomAdvertiser()

        every { advertiserService.getById(advertiser.id) } returns advertiser

        val result = campaignService.getAllByAdvertiserId(advertiser.id, 0, 0)

        result shouldBe emptyList()
    }

    "should update campaign successfully" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)
        val request = getRandomCampaignUpdateRequest()

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)

        val updatedCampaign = campaignService.update(advertiser.id, campaign.id!!, request)

        updatedCampaign shouldBe campaign.copy(
            impressionsLimit = request.impressionsLimit,
            clicksLimit = request.clicksLimit,
            costPerImpression = request.costPerImpression,
            costPerClick = request.costPerClick,
            adTitle = request.adTitle,
            adText = request.adText,
            startDate = request.startDate,
            endDate = request.endDate,
            attachmentId = request.attachmentId,
            targeting = request.targeting ?: campaign.targeting
        )
    }

    "should update campaign successfully with targeting = null" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)
        val request = getRandomCampaignUpdateRequest().copy(
            targeting = null
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)

        val updatedCampaign = campaignService.update(advertiser.id, campaign.id!!, request)

        updatedCampaign shouldBe campaign.copy(
            impressionsLimit = request.impressionsLimit,
            clicksLimit = request.clicksLimit,
            costPerImpression = request.costPerImpression,
            costPerClick = request.costPerClick,
            adTitle = request.adTitle,
            adText = request.adText,
            startDate = request.startDate,
            endDate = request.endDate,
            attachmentId = request.attachmentId,
            targeting = Campaign.Targeting()
        )
    }

    "should throw InvalidBodyException when campaign has started and trying to update start date, end date, impressions limit or clicks limit" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(startDate = 0)
        val request = getRandomCampaignUpdateRequest().copy(
            startDate = 0
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { timeService.getCurrentDateInt() } returns 1

        shouldThrow<InvalidBodyException> {
            campaignService.update(advertiser.id, campaign.id!!, request)
        }
    }

    "should throw InvalidBodyException when trying to make start date bigger than end date" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)
        val request = getRandomCampaignUpdateRequest().copy(
            startDate = 5,
            endDate = 1
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)

        shouldThrow<InvalidBodyException> {
            campaignService.update(advertiser.id, campaign.id!!, request)
        }
    }

    "should throw InvalidBodyException when trying to set start date to the past" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)
        val request = getRandomCampaignUpdateRequest().copy(
            startDate = 0
        )

        every { advertiserService.getById(advertiser.id) } returns advertiser
        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { timeService.getCurrentDateInt() } returns 1

        shouldThrow<InvalidBodyException> {
            campaignService.update(advertiser.id, campaign.id!!, request)
        }
    }

    "should delete campaign successfully" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id)

        every { campaignRepository.findById(campaign.id!!) } returns Optional.of(campaign)
        every { reviewRequestRepository.deleteAllByCampaignId(campaign.id!!) } just Runs
        every { adActionRepository.deleteAllByCampaignId(campaign.id!!) } just Runs
        every { campaignRepository.delete(campaign) } just Runs

        campaignService.delete(advertiser.id, campaign.id!!)

        verify { reviewRequestRepository.deleteAllByCampaignId(campaign.id!!) }
        verify { adActionRepository.deleteAllByCampaignId(campaign.id!!) }
        verify { campaignRepository.delete(campaign) }
    }

})
