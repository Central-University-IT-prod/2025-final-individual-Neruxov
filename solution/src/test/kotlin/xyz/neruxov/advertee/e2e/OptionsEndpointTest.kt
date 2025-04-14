package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import xyz.neruxov.advertee.data.options.request.ModerationOptions

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class OptionsEndpointTest : StringSpec({

    "should update moderation options" {
        TestSystem.validate {
            http {
                postAndExpectBodilessResponse(
                    "/options/moderation", body = ModerationOptions(
                        imageEnabled = false,
                        textEnabled = true
                    ).some()
                ) {
                    it.status shouldBe 204
                }

                get<ModerationOptions>("/options/moderation") { actual ->
                    actual shouldBe ModerationOptions(
                        imageEnabled = false,
                        textEnabled = true
                    )
                }
            }
        }
    }

    "should get moderation options" {
        TestSystem.validate {
            http {
                get<ModerationOptions>("/options/moderation") { actual ->
                    actual shouldBe ModerationOptions(
                        imageEnabled = false,
                        textEnabled = true
                    )
                }
            }
        }
    }

})