package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.generation.model.AdContentGenerationResult
import xyz.neruxov.advertee.service.AIService
import xyz.neruxov.advertee.service.AdGenerationService
import xyz.neruxov.advertee.service.AdvertiserService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdContentGenerationRequest
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaign
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class AdGenerationServiceTest : StringSpec({

    val aiService: AIService = mockk()
    val advertiserService: AdvertiserService = mockk()
    val campaignRepository: CampaignRepository = mockk()

    val adGenerationService = AdGenerationService(aiService, advertiserService, campaignRepository)

    "should generate ad content" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomAdContentGenerationRequest(advertiser.id)

        every { advertiserService.getById(request.advertiserId) } returns advertiser
        every {
            campaignRepository.findByModerationStatus(
                any(),
                any()
            )
        } returns listOf(getRandomCampaign(advertiser.id))
        every {
            aiService.callJson(
                any(),
                any(),
                any(),
                AdContentGenerationResult::class.java
            )
        } returns AdContentGenerationResult("test", "test", false)

        val result = adGenerationService.generateAdContent(request)

        result.title shouldBe "test"
        result.text shouldBe "test"
        result.rejected shouldBe false
    }

    "should throw NotFoundException if advertiser not found" {
        val request = getRandomAdContentGenerationRequest(UUID.randomUUID())

        every { advertiserService.getById(request.advertiserId) } throws NotFoundException("")

        shouldThrow<NotFoundException> {
            adGenerationService.generateAdContent(request)
        }
    }

})