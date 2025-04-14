package xyz.neruxov.advertee.unit

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.util.MimeType
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.data.moderation.model.ImageModerationResult
import xyz.neruxov.advertee.data.moderation.model.TextModerationResult
import xyz.neruxov.advertee.data.options.request.ModerationOptions
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import xyz.neruxov.advertee.service.AIService
import xyz.neruxov.advertee.service.ModerationService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign

class ModerationServiceTest : StringSpec({

    val aiService: AIService = mockk()
    val reviewRequestRepository: ReviewRequestRepository = mockk()
    val campaignRepository: CampaignRepository = mockk()

    val moderationService = ModerationService(aiService, reviewRequestRepository, campaignRepository)

    "should set moderation options" {
        val options = ModerationOptions(textEnabled = false, imageEnabled = true)

        moderationService.setModeration(options)

        moderationService.isTextEnabled() shouldBe false
        moderationService.isImageEnabled() shouldBe true
    }

    "should not moderate campaign if it's not awaiting moderation" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(moderationStatus = ModerationStatus.APPROVED)

        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                TextModerationResult::class.java
            )
        } returns TextModerationResult(
            safe = true,
            requiresManualReview = false,
            reason = null
        )

        moderationService.requestAutoModeration(campaign)

        // не могу проверять по moderateText, компилятору не нравится каст
        verify(exactly = 0) { campaignRepository.updateModerationStatus(campaign.id!!, any()) }
    }

    "should approve if safe" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(moderationStatus = ModerationStatus.AWAITING_MODERATION)

        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                TextModerationResult::class.java
            )
        } returns TextModerationResult(
            safe = true,
            requiresManualReview = false,
            reason = null
        )

        every { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.APPROVED) } just Runs

        moderationService.requestAutoModeration(campaign)

        verify { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.APPROVED) }
    }

    "should send to manual review if not safe" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(moderationStatus = ModerationStatus.AWAITING_MODERATION)

        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                TextModerationResult::class.java
            )
        } returns TextModerationResult(
            safe = false,
            requiresManualReview = true,
            reason = "reason"
        )

        every { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.ON_MANUAL_REVIEW) } just Runs
        every { reviewRequestRepository.save(any()) } returns mockk()

        moderationService.requestAutoModeration(campaign)

        verify { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.ON_MANUAL_REVIEW) }
        verify { reviewRequestRepository.save(any()) }
    }

    "should reject if not safe and doesn't require manual review" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(moderationStatus = ModerationStatus.AWAITING_MODERATION)

        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                TextModerationResult::class.java
            )
        } returns TextModerationResult(
            safe = false,
            requiresManualReview = false,
            reason = "reason"
        )

        every { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.REJECTED) } just Runs

        moderationService.requestAutoModeration(campaign)

        verify { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.REJECTED) }
    }

    "should work if the reason is null" {
        val advertiser = getRandomAdvertiser()
        val campaign = getRandomCampaign(advertiser.id).copy(moderationStatus = ModerationStatus.AWAITING_MODERATION)

        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                TextModerationResult::class.java
            )
        } returns TextModerationResult(
            safe = false,
            requiresManualReview = true,
            reason = null
        )

        every { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.ON_MANUAL_REVIEW) } just Runs
        every { reviewRequestRepository.save(any()) } returns mockk()

        moderationService.requestAutoModeration(campaign)

        verify { campaignRepository.updateModerationStatus(campaign.id!!, ModerationStatus.ON_MANUAL_REVIEW) }
        verify { reviewRequestRepository.save(any()) }
    }

    "should moderate image" {
        val image = byteArrayOf(1, 2, 3)
        val mimeType = MimeType.valueOf("image/png")

        every {
            aiService.callMediaJson(
                any(),
                any(),
                any(),
                ImageModerationResult::class.java
            )
        } returns ImageModerationResult(
            safe = true,
            reason = null
        )

        moderationService.moderateImage(image, mimeType)

        verify { aiService.callMediaJson(any(), any(), any(), ImageModerationResult::class.java) }
    }

})