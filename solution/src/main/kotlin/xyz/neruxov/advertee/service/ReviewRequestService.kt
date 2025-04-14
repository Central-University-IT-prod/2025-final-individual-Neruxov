package xyz.neruxov.advertee.service

import jakarta.transaction.Transactional
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.data.review.model.ReviewRequest
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class ReviewRequestService(
    private val reviewRequestRepository: ReviewRequestRepository,
    private val campaignRepository: CampaignRepository
) {

    fun getReviewRequests(page: Int, size: Int): List<ReviewRequest> {
        if (size == 0) return emptyList()

        val pageable = Pageable.ofSize(size).withPage(page)
        return reviewRequestRepository.findByVerdict(verdict = null, pageable)
    }

    fun getById(id: UUID): ReviewRequest {
        return reviewRequestRepository.findById(id)
            .orElseThrow { NotFoundException("Review request with id $id not found") }
    }

    @Transactional
    fun updateReviewRequest(id: UUID, verdict: Boolean) {
        val reviewRequest = getById(id)

        reviewRequestRepository.save(
            reviewRequest.copy(verdict = verdict)
        )

        campaignRepository.updateModerationStatus(
            id = reviewRequest.campaignId,
            moderationStatus = if (verdict) ModerationStatus.APPROVED else ModerationStatus.REJECTED
        )
    }

}