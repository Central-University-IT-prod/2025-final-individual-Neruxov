package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import xyz.neruxov.advertee.data.ad.dto.Ad
import xyz.neruxov.advertee.data.ad.request.AdClickRegisterRequest
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaignCreateRequest
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class AdEndpointTest : StringSpec({

    "should be able to get relevant ads" {
        TestSystem.validate {
            http {
                val client = getRandomClient()
                postAndExpectBody<Client>("/clients/bulk", body = listOf(client).some()) {
                    it.status shouldBe 201
                    it
                }

                val advertisers = (1..10).map { getRandomAdvertiser() }

                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = advertisers.some()) { actual ->
                    actual shouldBe advertisers
                }

                val campaigns = advertisers.map { advertiser ->
                    (1..10).map campaignMap@{
                        postAndExpectBody<Campaign>(
                            "/advertisers/${advertiser.id}/campaigns",
                            body = getRandomCampaignCreateRequest().copy(startDate = 0, targeting = null).some()
                        ) {
                            it.status shouldBe 201
                            return@campaignMap it.body()
                        }
                    }
                }.flatten() as List<Campaign>

                val seenCampaigns = mutableSetOf<Campaign>()
                for (i in 1..100) {
                    var ad: Ad? = null

                    get<Ad>("/ads?client_id=${client.id}") { actual ->
                        campaigns.map { it.id } shouldContain actual.id
                        seenCampaigns.add(campaigns.first { it.id == actual.id })
                        ad = actual
                    }

                    postAndExpectBodilessResponse(
                        "/ads/${ad!!.id}/click",
                        body = AdClickRegisterRequest(client.id).some()
                    ) {
                        it.status shouldBe 204
                    }
                }

                seenCampaigns.size shouldBe campaigns.size
            }
        }
    }

    // похожие тесты на этот есть еще в папочке algorithm_test :)

})