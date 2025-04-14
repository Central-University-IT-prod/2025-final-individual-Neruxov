package xyz.neruxov.advertee.service

import jakarta.transaction.Transactional
import org.springframework.ai.model.Media
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.data.moderation.model.ImageModerationResult
import xyz.neruxov.advertee.data.moderation.model.TextModerationResult
import xyz.neruxov.advertee.data.options.request.ModerationOptions
import xyz.neruxov.advertee.data.review.model.ReviewRequest
import xyz.neruxov.advertee.data.review.repo.ReviewRequestRepository

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class ModerationService(
    private val aiService: AIService,
    private val reviewRequestRepository: ReviewRequestRepository,
    private val campaignRepository: CampaignRepository
) {

    private final val moderationPrompt: String
    private final val moderationSchema: String
    private final val imageModerationPrompt: String
    private final val imageModerationSchema: String

    private var textEnabled = false
    private var imageEnabled = false

    fun setModeration(body: ModerationOptions) {
        textEnabled = body.textEnabled
        imageEnabled = body.imageEnabled
    }

    fun isTextEnabled() = textEnabled

    fun isImageEnabled() = imageEnabled

    @Async
    @Transactional
    fun requestAutoModeration(campaign: Campaign) {
        if (campaign.moderationStatus != ModerationStatus.AWAITING_MODERATION) return

        val result = moderateText(campaign.adText + "\n" + campaign.adTitle)

        var status = ModerationStatus.APPROVED
        if (!result.safe) {
            status = if (result.requiresManualReview) {
                reviewRequestRepository.save(
                    ReviewRequest(
                        campaignId = campaign.id!!,
                        aiReason = result.reason ?: "not provided",
                        adText = campaign.adText,
                        adTitle = campaign.adTitle
                    )
                )

                ModerationStatus.ON_MANUAL_REVIEW
            } else {
                ModerationStatus.REJECTED
            }
        }

        println(result)
        campaignRepository.updateModerationStatus(campaign.id!!, status)
    }

    fun moderateImage(image: ByteArray, mimeType: MimeType): ImageModerationResult {
        return aiService.callMediaJson(
            imageModerationPrompt,
            Media.builder()
                .data(image)
                .mimeType(mimeType)
                .build(),
            imageModerationSchema,
            ImageModerationResult::class.java
        )
    }

    private fun moderateText(text: String): TextModerationResult {
        return aiService.callJson(
            moderationPrompt,
            text,
            moderationSchema,
            TextModerationResult::class.java
        )
    }

    private fun readResource(resource: ClassPathResource): String {
        return resource.inputStream.bufferedReader().use { it.readText() }
    }

    init {
        moderationPrompt = readResource(ClassPathResource("moderation/text/prompt.txt"))
        moderationSchema = readResource(ClassPathResource("moderation/text/schema.json"))
        imageModerationPrompt = readResource(ClassPathResource("moderation/image/prompt.txt"))
        imageModerationSchema = readResource(ClassPathResource("moderation/image/schema.json"))
    }

}