package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.data.mlscore.request.MLScoreRequest
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class MLScoreEndpointTest : StringSpec({

    "should put ml score" {
        TestSystem.validate {
            http {
                val client = getRandomClient()
                postAndExpectJson<List<Client>>("/clients/bulk", body = listOf(client).some()) { actual ->
                    actual shouldBe listOf(client)
                }

                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                postAndExpectBodilessResponse(
                    "/ml-scores",
                    body = MLScoreRequest(clientId = client.id, advertiserId = advertiser.id, score = 50).some()
                ) {
                    it.status shouldBe 200
                }
            }
        }
    }

    "should throw 404 if client not found" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                postAndExpectBody<MLScoreRequest>(
                    "/ml-scores",
                    body = MLScoreRequest(clientId = UUID.randomUUID(), advertiserId = advertiser.id, score = 50).some()
                ) { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should throw 404 if advertiser not found" {
        TestSystem.validate {
            http {
                val client = getRandomClient()
                postAndExpectJson<List<Client>>("/clients/bulk", body = listOf(client).some()) { actual ->
                    actual shouldBe listOf(client)
                }

                postAndExpectBody<MLScoreRequest>(
                    "/ml-scores",
                    body = MLScoreRequest(clientId = client.id, advertiserId = UUID.randomUUID(), score = 50).some()
                ) { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

})