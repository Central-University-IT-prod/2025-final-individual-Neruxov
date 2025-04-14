package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class AdvertiserEndpointTest : StringSpec({

    "should create advertisers" {
        TestSystem.validate {
            http {
                val advertisers = (1..10).map { getRandomAdvertiser() }

                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = advertisers.some()) { actual ->
                    actual shouldBe advertisers
                }
            }
        }
    }

    "should throw 400 if advertiser is invalid" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser().copy(name = "")

                postAndExpectBody<Advertiser>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should get advertiser by id" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()

                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                get<Advertiser>("/advertisers/${advertiser.id}") { actual ->
                    actual shouldBe advertiser
                }
            }
        }
    }

    "should throw 404 if advertiser not found" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/00000000-0000-0000-0000-000000000000") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should throw 400 if advertiser id is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/invalid") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should update advertiser" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()

                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val updatedAdvertiser = advertiser.copy(name = "askdjakldjkalsjdkla")

                postAndExpectJson<List<Advertiser>>(
                    "/advertisers/bulk",
                    body = listOf(updatedAdvertiser).some()
                ) { actual ->
                    actual shouldBe listOf(updatedAdvertiser)
                }
            }
        }
    }

})