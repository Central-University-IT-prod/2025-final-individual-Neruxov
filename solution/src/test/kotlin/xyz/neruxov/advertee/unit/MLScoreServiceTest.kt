package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.data.client.repo.ClientRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.mlscore.model.MLScore
import xyz.neruxov.advertee.data.mlscore.repo.MLScoreRepository
import xyz.neruxov.advertee.data.mlscore.request.MLScoreRequest
import xyz.neruxov.advertee.service.MLScoreService
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import xyz.neruxov.advertee.util.ModelGenerators.getRandomClient
import xyz.neruxov.advertee.util.ModelGenerators.getRandomMLScoreRequest
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class MLScoreServiceTest : StringSpec({

    val advertiserRepository: AdvertiserRepository = mockk()
    val clientRepository: ClientRepository = mockk()
    val mlScoreRepository: MLScoreRepository = mockk()

    val mlScoreService = MLScoreService(clientRepository, mlScoreRepository, advertiserRepository)

    "should add ml scores" {
        val client = getRandomClient()
        val advertiser = getRandomAdvertiser()
        val request = getRandomMLScoreRequest(clientId = client.id, advertiserId = advertiser.id)

        val expectedModel = request.toExpectedModel()

        every { clientRepository.findById(client.id) } returns Optional.of(client)
        every { advertiserRepository.findById(advertiser.id) } returns Optional.of(advertiser)
        every { mlScoreRepository.save(expectedModel) } returns expectedModel

        mlScoreService.put(request)
        verify { mlScoreRepository.save(expectedModel) }
    }

    "should throw NotFoundException if client not found" {
        val advertiser = getRandomAdvertiser()
        val request = getRandomMLScoreRequest(UUID.randomUUID(), advertiser.id)

        every { clientRepository.findById(request.clientId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            mlScoreService.put(request)
        }
    }

    "should throw NotFoundException if advertiser not found" {
        val client = getRandomClient()
        val request = getRandomMLScoreRequest(client.id, UUID.randomUUID())

        every { clientRepository.findById(request.clientId) } returns Optional.of(client)
        every { advertiserRepository.findById(request.advertiserId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            mlScoreService.put(request)
        }
    }

}) {

    companion object {

        fun MLScoreRequest.toExpectedModel() = MLScore(
            id = MLScore.Id(
                clientId = clientId,
                advertiserId = advertiserId,
            ),
            score = score
        )

    }

}