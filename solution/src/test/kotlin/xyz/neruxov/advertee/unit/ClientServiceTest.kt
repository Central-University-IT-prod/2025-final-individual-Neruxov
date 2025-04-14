package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import xyz.neruxov.advertee.data.client.repo.ClientRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.service.ClientService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class ClientServiceTest : StringSpec({

    val clientRepository: ClientRepository = mockk()
    val clientService = ClientService(clientRepository)

    "should create (update) client" {
        val client = getRandomClient()

        every { clientRepository.save(client) } returns client

        val result = clientService.create(client)

        result shouldBe client
        verify { clientRepository.save(client) }
    }

    "should get client by id" {
        val client = getRandomClient()

        every { clientRepository.findById(client.id) } returns Optional.of(client)

        val result = clientService.getById(client.id)

        result shouldBe client
    }

    "should throw exception if client not found" {
        val clientId = UUID.randomUUID()

        every { clientRepository.findById(clientId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            clientService.getById(clientId)
        }
    }

})