package xyz.neruxov.advertee.service

import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.generation.model.AdContentGenerationResult
import xyz.neruxov.advertee.data.generation.request.AdContentGenerationRequest
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class AdGenerationService(
    private val aiService: AIService,
    private val advertiserService: AdvertiserService,
    private val campaignRepository: CampaignRepository
) {

    private final val generationPrompt: String

    private final val generationSchema: String

    fun generateAdContent(body: AdContentGenerationRequest): AdContentGenerationResult {
        val advertiser = advertiserService.getById(body.advertiserId)
        val campaigns = campaignRepository.findByModerationStatus(ModerationStatus.APPROVED, Limit.of(5))

        return generateAdContent(advertiser = advertiser, request = body.request, examples = campaigns)
    }

    private fun generateAdContent(
        advertiser: Advertiser,
        request: String,
        examples: List<Campaign>
    ): AdContentGenerationResult {
        return aiService.callJson(
            generationPrompt,
            """
                Рекламодатель: ${advertiser.name}
                Запрос: $request
                Примеры:
            """.trimIndent() +
                    examples.joinToString("\n\n")
                    { "Заголовок: ${it.adTitle}\nПодзаголовок: ${it.adText}" },
            generationSchema,
            AdContentGenerationResult::class.java
        )
    }

    private fun readResource(resource: ClassPathResource): String {
        return resource.inputStream.bufferedReader().use { it.readText() }
    }

    init {
        generationPrompt = readResource(ClassPathResource("generation/prompt.txt"))
        generationSchema = readResource(ClassPathResource("generation/schema.json"))
    }

}