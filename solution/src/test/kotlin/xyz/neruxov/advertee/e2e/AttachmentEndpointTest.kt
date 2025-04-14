package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.StoveMultiPartContent
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.perProject
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.io.ClassPathResource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.attachment.model.Attachment
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomCampaignCreateRequest

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class AttachmentEndpointTest : StringSpec({

    val testImageBytes = ClassPathResource("nocode.png").file.readBytes()

    listener(s3.perProject())

    "should upload attachment" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                var attachment: Attachment? = null

                postMultipartAndExpectResponse<Attachment>(
                    "/attachments/advertisers/${advertiser.id}",
                    listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                ) {
                    it.status shouldBe 201
                    attachment = it.body()
                }

                get<Attachment>("/attachments/${attachment!!.id}") { actual ->
                    actual shouldBe attachment
                }

                getResponse<Any>("/attachments/${attachment!!.id}/content") { actual ->
                    actual.body shouldNotBeNull { }
                    actual.status shouldBe 200
                    actual.headers.keys shouldContain "content-disposition"
                }
            }
        }
    }

    "should throw 404 if attachment has invalid advertiser" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                postMultipartAndExpectResponse<Attachment>(
                    "/attachments/advertisers/00000000-0000-0000-0000-000000000000",
                    listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                ) {
                    it.status shouldBe 404
                }
            }
        }
    }

    "should delete attachment" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                var attachment: Attachment? = null

                postMultipartAndExpectResponse<Attachment>(
                    "/attachments/advertisers/${advertiser.id}",
                    listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                ) {
                    it.status shouldBe 201
                    attachment = it.body()
                }

                deleteAndExpectBodilessResponse("/attachments/${attachment!!.id}") {
                    it.status shouldBe 204
                }

                getResponse<Any>("/attachments/${attachment!!.id}") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should get all by advertiser id paged" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                val attachments = mutableListOf<Attachment>()

                (1..10).forEach { _ ->
                    postMultipartAndExpectResponse<Attachment>(
                        "/attachments/advertisers/${advertiser.id}",
                        listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                    ) {
                        it.status shouldBe 201
                        attachments.add(it.body())
                    }
                }

                get<List<Attachment>>("/attachments/advertisers/${advertiser.id}?page=0&size=10") { actual ->
                    actual shouldBe attachments
                }

                get<List<Attachment>>("/attachments/advertisers/${advertiser.id}?page=0&size=5") { actual ->
                    actual shouldBe attachments.take(5)
                }

                get<List<Attachment>>("/attachments/advertisers/${advertiser.id}?page=1&size=5") { actual ->
                    actual shouldBe attachments.drop(5)
                }

                get<List<Attachment>>("/attachments/advertisers/${advertiser.id}?page=1&size=3") { actual ->
                    actual shouldBe attachments.drop(3).take(3)
                }
            }
        }
    }

    "should throw 400 if page is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/attachments/advertisers/00000000-0000-0000-0000-000000000000?page=-1&size=10") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if size is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/attachments/advertisers/00000000-0000-0000-0000-000000000000?page=0&size=-1") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 400 if advertiser id is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/attachments/advertisers/invalid") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should throw 404 if advertiser not found" {
        TestSystem.validate {
            http {
                getResponse<Any>("/attachments/advertisers/00000000-0000-0000-0000-000000000000") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should throw 404 if attachment not found" {
        TestSystem.validate {
            http {
                getResponse<Any>("/attachments/00000000-0000-0000-0000-000000000000") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should create campaign with attachment" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                var attachment: Attachment? = null

                postMultipartAndExpectResponse<Attachment>(
                    "/attachments/advertisers/${advertiser.id}",
                    listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                ) {
                    it.status shouldBe 201
                    attachment = it.body()
                }

                val request = getRandomCampaignCreateRequest().copy(attachmentId = attachment!!.id)

                postAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) {
                    it.status shouldBe 201
                    it.body()
                }
            }
        }
    }

    "should throw 409 if attachment is used in some campaign" {
        TestSystem.validate {
            http {
                val advertiser = getRandomAdvertiser()
                postAndExpectJson<List<Advertiser>>("/advertisers/bulk", body = listOf(advertiser).some()) { actual ->
                    actual shouldBe listOf(advertiser)
                }

                var attachment: Attachment? = null

                postMultipartAndExpectResponse<Attachment>(
                    "/attachments/advertisers/${advertiser.id}",
                    listOf(StoveMultiPartContent.File("file", "nocode.png", testImageBytes, "image/png")),
                ) {
                    it.status shouldBe 201
                    attachment = it.body()
                }

                val request = getRandomCampaignCreateRequest().copy(attachmentId = attachment!!.id)

                postAndExpectBody<Campaign>(
                    "/advertisers/${advertiser.id}/campaigns",
                    body = request.some()
                ) {
                    it.status shouldBe 201
                    it.body()
                }

                deleteAndExpectBodilessResponse(
                    "/attachments/${attachment!!.id}"
                ) {
                    it.status shouldBe 409
                }
            }
        }
    }

}) {

    companion object {

        @ServiceConnection
        private val s3 = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest")
        )
            .withServices(LocalStackContainer.Service.S3)
            .apply {
                start()
                execInContainer("/bin/bash", "-c", "awslocal s3 mb s3://test-bucket")
                System.setProperty(
                    "spring.cloud.aws.s3.endpoint",
                    getEndpointOverride(LocalStackContainer.Service.S3).toString()
                )
                System.setProperty("spring.cloud.aws.s3.bucket", "test-bucket")
                System.setProperty("spring.cloud.aws.credentials.access-key", accessKey)
                System.setProperty("spring.cloud.aws.credentials.secret-key", secretKey)
            }

    }

}