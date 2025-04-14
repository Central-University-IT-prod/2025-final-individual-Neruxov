package xyz.neruxov.advertee.e2e

import arrow.core.some
import com.trendyol.stove.testing.e2e.http.http
import com.trendyol.stove.testing.e2e.system.TestSystem
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.perProject
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@SpringBootTest
class ClientEndpointTest : StringSpec({

    listener(postgres.perProject())

    "should create clients" {
        TestSystem.validate {
            http {
                val clients = (1..10).map { getRandomClient() }

                postAndExpectJson<List<Client>>("/clients/bulk", body = clients.some()) { actual ->
                    actual shouldBe clients
                }
            }
        }
    }

    "should throw 400 if client is invalid" {
        TestSystem.validate {
            http {
                val client = getRandomClient().copy(age = -69)

                postAndExpectBody<Client>("/clients/bulk", body = listOf(client).some()) { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should get client by id" {
        TestSystem.validate {
            http {
                val client = getRandomClient()

                postAndExpectJson<List<Client>>("/clients/bulk", body = listOf(client).some()) { actual ->
                    actual shouldBe listOf(client)
                }

                get<Client>("/clients/${client.id}") { actual ->
                    actual shouldBe client
                }
            }
        }
    }

    "should throw 404 if client not found" {
        TestSystem.validate {
            http {
                getResponse<Any>("/clients/00000000-0000-0000-0000-000000000000") { actual ->
                    actual.status shouldBe 404
                }
            }
        }
    }

    "should throw 400 if client id is invalid" {
        TestSystem.validate {
            http {
                getResponse<Any>("/clients/invalid") { actual ->
                    actual.status shouldBe 400
                }
            }
        }
    }

    "should update client" {
        TestSystem.validate {
            http {
                val client = getRandomClient()

                postAndExpectJson<List<Client>>("/clients/bulk", body = listOf(client).some()) { actual ->
                    actual shouldBe listOf(client)
                }

                val updatedClient = client.copy(login = "test")

                postAndExpectJson<List<Client>>("/clients/bulk", body = listOf(updatedClient).some()) { actual ->
                    actual shouldBe listOf(updatedClient)
                }
            }
        }
    }

}) {

    companion object {

        @ServiceConnection
        private val postgres = PostgreSQLContainer("postgres:latest")
            .apply {
                start()
                System.setProperty("spring.datasource.url", jdbcUrl)
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }

    }

}