package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.service.AdvertiserService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class AdvertiserServiceTest : StringSpec({

    val advertiserRepository: AdvertiserRepository = mockk()
    val advertiserService = AdvertiserService(advertiserRepository)

    fun getRandomAdvertiser() = Advertiser(
        id = UUID.randomUUID(),
        name = "test"
    )

    "should create (update) advertiser" {
        val advertiser = getRandomAdvertiser()

        every { advertiserRepository.save(advertiser) } returns advertiser

        val result = advertiserService.create(advertiser)

        result shouldBe advertiser
        verify { advertiserRepository.save(advertiser) }
    }

    "should get advertiser by id" {
        val advertiser = getRandomAdvertiser()

        every { advertiserRepository.findById(advertiser.id) } returns Optional.of(advertiser)

        val result = advertiserService.getById(advertiser.id)

        result shouldBe advertiser
    }

    "should throw exception if advertiser not found" {
        val advertiserId = UUID.randomUUID()

        every { advertiserRepository.findById(advertiserId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            advertiserService.getById(advertiserId)
        }
    }

})