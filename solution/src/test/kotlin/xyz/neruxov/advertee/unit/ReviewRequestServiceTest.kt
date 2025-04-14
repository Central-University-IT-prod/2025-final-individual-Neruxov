package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import xyz.neruxov.advertee.service.ReviewRequestService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomReviewRequest
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class ReviewRequestServiceTest : StringSpec({

    val reviewRequestRepository: ReviewRequestRepository = mockk()
    val campaignRepository: CampaignRepository = mockk()

    val reviewRequestService = ReviewRequestService(reviewRequestRepository, campaignRepository)

    "should return paged review requests" {
        val requests = (0..10).map { getRandomReviewRequest() }

        every { reviewRequestRepository.findByVerdict(null, any()) } returns requests

        val result = reviewRequestService.getReviewRequests(0, 10)

        result shouldBe requests
    }

    "should return empty list if size = 0" {
        val result = reviewRequestService.getReviewRequests(0, 0)

        result shouldBe emptyList()
    }

    "should approve review request" {
        val request = getRandomReviewRequest()
        val expectedRequest = request.copy(verdict = true)

        every { reviewRequestRepository.findById(request.id!!) } returns Optional.of(request)
        every { reviewRequestRepository.save(expectedRequest) } returns expectedRequest

        every { campaignRepository.updateModerationStatus(request.campaignId, any()) } just Runs

        reviewRequestService.updateReviewRequest(request.id!!, true)

        verify { reviewRequestRepository.save(expectedRequest) }
        verify { campaignRepository.updateModerationStatus(request.campaignId, ModerationStatus.APPROVED) }
    }

    "should reject review request" {
        val request = getRandomReviewRequest()
        val expectedRequest = request.copy(verdict = false)

        every { reviewRequestRepository.findById(request.id!!) } returns Optional.of(request)
        every { reviewRequestRepository.save(expectedRequest) } returns expectedRequest

        every { campaignRepository.updateModerationStatus(request.campaignId, any()) } just Runs

        reviewRequestService.updateReviewRequest(request.id!!, false)

        verify { reviewRequestRepository.save(expectedRequest) }
        verify { campaignRepository.updateModerationStatus(request.campaignId, ModerationStatus.REJECTED) }
    }

    "should throw NotFoundException if review request not found" {
        val requestId = UUID.randomUUID()

        every { reviewRequestRepository.findById(requestId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            reviewRequestService.getById(requestId)
        }
    }

})