package xyz.neruxov.advertee.service

import jakarta.transaction.Transactional
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.campaign.request.CampaignCreateRequest
import xyz.neruxov.advertee.data.campaign.request.CampaignUpdateRequest
import xyz.neruxov.advertee.data.error.impl.InvalidBodyException
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository
import java.util.*
import kotlin.jvm.optionals.getOrElse

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val timeService: TimeService,
    private val advertiserService: AdvertiserService,
    private val adActionRepository: AdActionRepository,
    private val attachmentService: AttachmentService,
    private val moderationService: ModerationService,
    private val reviewRequestRepository: ReviewRequestRepository
) {

    fun create(advertiserId: UUID, body: CampaignCreateRequest): Campaign {
        advertiserService.getById(advertiserId)

        if (body.clicksLimit > body.impressionsLimit) {
            throw InvalidBodyException("Clicks limit must be less than or equal to impressions limit")
        }

        if (body.startDate > body.endDate) {
            throw InvalidBodyException("Start date must be less than end date")
        }

        val currentDate = timeService.getCurrentDateInt()
        if (body.startDate < currentDate) {
            throw InvalidBodyException("Start date must be greater than current date")
        }

        if (body.attachmentId != null) {
            attachmentService.getById(body.attachmentId)
        }

        val campaign = campaignRepository.save(
            Campaign(
                advertiserId = advertiserId,
                impressionsLimit = body.impressionsLimit,
                clicksLimit = body.clicksLimit,
                costPerImpression = body.costPerImpression,
                costPerClick = body.costPerClick,
                adTitle = body.adTitle,
                adText = body.adText,
                startDate = body.startDate,
                endDate = body.endDate,
                attachmentId = body.attachmentId,
                targeting = body.targeting ?: Campaign.Targeting(),
                moderationStatus = if (moderationService.isTextEnabled()) ModerationStatus.AWAITING_MODERATION else ModerationStatus.UNMODERATED
            )
        )

        if (moderationService.isTextEnabled()) {
            moderationService.requestAutoModeration(campaign)
        }

        return campaign
    }

    fun getAllByAdvertiserId(advertiserId: UUID, page: Int, size: Int): List<Campaign> {
        if (size == 0) return emptyList()

        advertiserService.getById(advertiserId)

        val pageable = Pageable.ofSize(size).withPage(page)
        return campaignRepository.findAllByAdvertiserId(advertiserId, pageable)
    }

    fun getById(advertiserId: UUID, id: UUID): Campaign {
        val campaign =
            campaignRepository.findById(id).getOrElse { throw NotFoundException("Campaign with id $id not found") }

        if (campaign.advertiserId != advertiserId) {
            throw NotFoundException("Campaign with id $id not found")
        }

        return campaign
    }

    // todo: чекнуть еще раз логику тута
    fun update(advertiserId: UUID, id: UUID, body: CampaignUpdateRequest): Campaign {
        val campaign = getById(advertiserId, id)

        val currentDate = timeService.getCurrentDateInt()

        if (campaign.startDate < currentDate && (body.impressionsLimit != campaign.impressionsLimit || body.clicksLimit != campaign.clicksLimit || body.startDate != campaign.startDate || body.endDate != campaign.endDate)) {
            throw InvalidBodyException("Cannot update impressions limit, clicks limit, start date or end date of a campaign that has already started")
        }

        if (body.startDate < currentDate) {
            throw InvalidBodyException("Start date must be greater or equal to the current date")
        }

        if (body.startDate > body.endDate) {
            throw InvalidBodyException("Start date must be less than end date")
        }

        if (body.attachmentId != null) {
            attachmentService.getById(body.attachmentId)
        }

        val newCampaign = Campaign(
            id = campaign.id,
            advertiserId = campaign.advertiserId,
            impressionsLimit = body.impressionsLimit,
            clicksLimit = body.clicksLimit,
            costPerImpression = body.costPerImpression,
            costPerClick = body.costPerClick,
            adTitle = body.adTitle,
            adText = body.adText,
            startDate = body.startDate,
            endDate = body.endDate,
            attachmentId = body.attachmentId,
            targeting = body.targeting ?: Campaign.Targeting(),
            moderationStatus = if (moderationService.isTextEnabled()) ModerationStatus.AWAITING_MODERATION else ModerationStatus.UNMODERATED
        )

        if (moderationService.isTextEnabled()) {
            moderationService.requestAutoModeration(newCampaign)
        }

        return campaignRepository.save(newCampaign)
    }

    @Transactional
    fun delete(advertiserId: UUID, id: UUID) {
        val campaign = getById(advertiserId, id)

        reviewRequestRepository.deleteAllByCampaignId(id)
        adActionRepository.deleteAllByCampaignId(id)
        campaignRepository.delete(campaign)
    }

}