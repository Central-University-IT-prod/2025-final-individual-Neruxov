package xyz.neruxov.advertee.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import xyz.neruxov.advertee.data.error.impl.InvalidBodyException
import xyz.neruxov.advertee.data.time.dto.TimeDto
import xyz.neruxov.advertee.data.time.model.Time
import xyz.neruxov.advertee.data.time.repo.TimeRepository
import xyz.neruxov.advertee.service.MetricsService
import xyz.neruxov.advertee.service.TIME_DAY_DB_ID
import xyz.neruxov.advertee.service.TimeService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class TimeServiceTest : StringSpec({

    val timeRepository: TimeRepository = mockk()
    val metricsService: MetricsService = mockk()

    every { timeRepository.findById(TIME_DAY_DB_ID) } returns Optional.of(Time(id = TIME_DAY_DB_ID, date = 0))

    val timeService = TimeService(timeRepository, metricsService)

    "should update time" {
        val day = 1
        val time = Time(id = TIME_DAY_DB_ID, date = day)

        every { timeRepository.findById(TIME_DAY_DB_ID) } returns Optional.of(time)
        every { timeRepository.save(time) } returns time

        every { metricsService.updateMaxDate(newDate = day) } just Runs

        val result = timeService.setCurrentDate(day)

        result shouldBe TimeDto(day)

        verify { timeRepository.save(time) }
        verify { metricsService.updateMaxDate(newDate = day) }
    }

    "should throw exception if date is in the past" {
        val day = 1
        val time = Time(id = TIME_DAY_DB_ID, date = day)

        every { timeRepository.findById(TIME_DAY_DB_ID) } returns Optional.of(time)

        shouldThrow<InvalidBodyException> {
            timeService.setCurrentDate(day - 1)
        }
    }

    "should get current date" {
        val day = 1
        val time = Time(id = TIME_DAY_DB_ID, date = day)

        every { timeRepository.findById(TIME_DAY_DB_ID) } returns Optional.of(time)

        val result = timeService.getCurrentDateInt()

        result shouldBe day
    }

})