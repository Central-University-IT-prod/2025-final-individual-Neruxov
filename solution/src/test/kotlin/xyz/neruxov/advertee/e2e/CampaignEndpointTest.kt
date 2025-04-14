package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaignCreateRequest
import xyz.neruxov.advertee.util.ModelGenerators.toCampaign

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class CampaignEndpointTest : StringSpec({

    "should create campaign" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest()
                var expectedCampaign = request.toCampaign(advertiserId = advertiser.id)

                postAndExpectJson<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    expectedCampaign = expectedCampaign.copy(id = actual.id)
                    actual shouldBe expectedCampaign
                }

                get<Campaign>("/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}") { actual ->
                    actual shouldBe expectedCampaign
                }
            }
        }
    }

    "should return 400 if campaign is invalid" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest().copy(impressionsLimit = -1)
                postAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should update campaign" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest()
                var expectedCampaign = request.toCampaign(advertiserId = advertiser.id)

                postAndExpectJson<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    expectedCampaign = expectedCampaign.copy(id = actual.id)
                    actual shouldBe expectedCampaign
                }

                val updateRequest = getRandomCampaignCreateRequest()
                expectedCampaign = updateRequest.toCampaign(advertiserId = advertiser.id).copy(id = expectedCampaign.id)

                putAndExpectJson<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}",
                    body = updateRequest.some()
                ) { actual ->
                    actual shouldBe expectedCampaign
                }

                get<Campaign>("/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}") { actual ->
                    actual shouldBe expectedCampaign
                }
            }
        }
    }

    "should throw 400 if trying to update dates of an active campaign" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest().copy(startDate = 0)
                var expectedCampaign = request.toCampaign(advertiserId = advertiser.id)

                postAndExpectJson<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    expectedCampaign = expectedCampaign.copy(id = actual.id)
                    actual shouldBe expectedCampaign
                }

                val requestDate = mapOf("current_date" to 1)
                postAndExpectBody<Map<String, Int>>("/time/advance", body = requestDate.some()) { actual ->
                    actual.status shouldBe 200
                    actual.body() shouldBe requestDate
                }

                val updateRequest = getRandomCampaignCreateRequest().copy(startDate = 2, endDate = 6)
                putAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}",
                    body = updateRequest.some()
                ) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if some fields are missing" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest().copy(adTitle = "")
                postAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 404 if campaign not found" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/00000000-0000-0000-0000-000000000000/campaigns/00000000-0000-0000-0000-000000000000") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should throw 400 if campaign id is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/invalid/campaigns/invalid") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if campaign is invalid" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest().copy(impressionsLimit = -1)
                postAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if advertiser id is invalid" {
        TestSystem.validate {
            http {
                val request = getRandomCampaignCreateRequest()
                postAndExpectBody<Campaign>("/advertisers/invalid/campaigns", body = request.some()) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if advertiser not found" {
        TestSystem.validate {
            http {
                val request = getRandomCampaignCreateRequest()
                postAndExpectBody<Campaign>(
                    "/advertisers/00000000-0000-0000-0000-000000000000/campaigns",
                    body = request.some()
                ) { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should get campaigns paged by advertiser id" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val campaigns =
                    (1..10).map { getRandomCampaignCreateRequest().toCampaign(advertiserId = advertiser.id) }
                val newCampaigns = mutableListOf<Campaign>()

                campaigns.forEach {
                    postAndExpectJson<Campaign>("/advertisers/${advertiser.id}/campaigns", body = it.some()) { actual ->
                        it.copy(id = actual.id) shouldBe actual
                        newCampaigns.add(actual)
                    }
                }

                get<List<Campaign>>("/advertisers/${advertiser.id}/campaigns") { actual ->
                    actual shouldBe newCampaigns
                }

                get<List<Campaign>>("/advertisers/${advertiser.id}/campaigns?size=5&page=0") { actual ->
                    actual shouldBe newCampaigns.subList(0, 5)
                }

                get<List<Campaign>>("/advertisers/${advertiser.id}/campaigns?size=5&page=1") { actual ->
                    actual shouldBe newCampaigns.subList(5, 10)
                }

                get<List<Campaign>>("/advertisers/${advertiser.id}/campaigns?size=5&page=2") { actual ->
                    actual shouldBe emptyList()
                }
            }
        }
    }

    "should throw 400 if size is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/00000000-0000-0000-0000-000000000000/campaigns?size=-1") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if page is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/advertisers/00000000-0000-0000-0000-000000000000/campaigns?page=-1") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should delete campaign" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val request = getRandomCampaignCreateRequest()
                var expectedCampaign = request.toCampaign(advertiserId = advertiser.id)

                postAndExpectJson<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) { actual ->
                    expectedCampaign = expectedCampaign.copy(id = actual.id)
                    actual shouldBe expectedCampaign
                }

                deleteAndExpectBodilessResponse("/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}") {
                    it.status shouldBe 204
                }

                getResponse<Any>("/advertisers/${advertiser.id}/campaigns/${expectedCampaign.id}") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

})