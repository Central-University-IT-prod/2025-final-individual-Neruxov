package xyz.neruxov.advertee.util

import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.request.CampaignCreateRequest
import xyz.neruxov.advertee.data.campaign.request.CampaignUpdateRequest
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.data.generation.request.AdContentGenerationRequest
import xyz.neruxov.advertee.data.mlscore.request.MLScoreRequest
import xyz.neruxov.advertee.data.review.model.ReviewRequest
import xyz.neruxov.advertee.util.enum.Gender
import xyz.neruxov.advertee.util.enum.GenderFilter
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
object ModelGenerators {

    fun getRandomAdvertiser() = Advertiser(
        id = randomUUID(),
        name = randomString()
    )

    fun getRandomClient() = Client(
        id = randomUUID(),
        login = randomString(),
        age = randomInt(),
        location = randomString(),
        gender = Gender.MALE
    )

    fun getRandomMLScoreRequest(advertiserId: UUID, clientId: UUID) = MLScoreRequest(
        advertiserId = advertiserId,
        clientId = clientId,
        score = randomInt()
    )

    fun getRandomCampaignCreateRequest() = CampaignCreateRequest(
        impressionsLimit = randomInt(10, 100),
        clicksLimit = randomInt(0, 10),
        costPerImpression = randomFloat(),
        costPerClick = randomFloat(),
        adTitle = randomString(),
        adText = randomString(),
        startDate = 1,
        endDate = 5,
        attachmentId = null,
        targeting = Campaign.Targeting(
            gender = GenderFilter.MALE,
            ageFrom = 0,
            ageTo = 100,
            location = randomString()
        )
    )

    fun getRandomCampaignUpdateRequest() = CampaignUpdateRequest(
        impressionsLimit = randomInt(),
        clicksLimit = randomInt(),
        costPerImpression = randomFloat(),
        costPerClick = randomFloat(),
        adTitle = randomString(),
        adText = randomString(),
        startDate = 1,
        endDate = 5,
        attachmentId = null,
        targeting = Campaign.Targeting(
            gender = GenderFilter.MALE,
            ageFrom = 0,
            ageTo = 100,
            location = randomString()
        )
    )

    fun getRandomCampaign(advertiserId: UUID) = Campaign(
        id = randomUUID(),
        advertiserId = advertiserId,
        impressionsLimit = randomInt(),
        clicksLimit = randomInt(),
        costPerImpression = randomFloat(),
        costPerClick = randomFloat(),
        adTitle = randomString(),
        adText = randomString(),
        startDate = 1,
        endDate = 5,
        attachmentId = null,
        targeting = Campaign.Targeting(
            gender = GenderFilter.MALE,
            ageFrom = 0,
            ageTo = 100,
            location = randomString()
        )
    )

    fun getRandomReviewRequest() = ReviewRequest(
        id = randomUUID(),
        campaignId = randomUUID(),
        adTitle = randomString(),
        adText = randomString(),
        aiReason = randomString()
    )

    fun getRandomAdContentGenerationRequest(advertiserId: UUID) = AdContentGenerationRequest(
        advertiserId = advertiserId,
        request = randomString()
    )

    fun getRandomAdAction(campaignId: UUID, clientId: UUID, type: AdAction.Type) = AdAction(
        id = AdAction.Id(
            campaignId = campaignId,
            clientId = clientId,
            type = type
        ),
        cost = randomFloat(),
        date = 1
    )

    fun CampaignCreateRequest.toCampaign(advertiserId: UUID) = Campaign(
        id = randomUUID(),
        advertiserId = advertiserId,
        impressionsLimit = impressionsLimit,
        clicksLimit = clicksLimit,
        costPerImpression = costPerImpression,
        costPerClick = costPerClick,
        adTitle = adTitle,
        adText = adText,
        startDate = startDate,
        endDate = endDate,
        attachmentId = attachmentId,
        targeting = targeting ?: Campaign.Targeting()
    )

    fun CampaignUpdateRequest.toCampaign(advertiserId: UUID) = Campaign(
        id = randomUUID(),
        advertiserId = advertiserId,
        impressionsLimit = impressionsLimit,
        clicksLimit = clicksLimit,
        costPerImpression = costPerImpression,
        costPerClick = costPerClick,
        adTitle = adTitle,
        adText = adText,
        startDate = startDate,
        endDate = endDate,
        attachmentId = attachmentId,
        targeting = targeting ?: Campaign.Targeting()
    )

    private fun randomUUID() = UUID.randomUUID()

    private fun randomString(length: Int = 8) = (1..length).map { ('a'..'z').random() }.joinToString("")

    private fun randomInt(a: Int = 1, b: Int = 100) = (a..b).random()

    private fun randomFloat() = randomInt().toFloat()

}