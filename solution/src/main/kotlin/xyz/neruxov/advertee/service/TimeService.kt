package xyz.neruxov.advertee.service

import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.error.impl.InvalidBodyException
import xyz.neruxov.advertee.data.time.dto.TimeDto
import xyz.neruxov.advertee.data.time.model.Time
import xyz.neruxov.advertee.data.time.repo.TimeRepository

const val TIME_DAY_DB_ID: Int = 1

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 * гениальный код, я знаю
 */
@Service
class TimeService(
    val timeRepository: TimeRepository,
    private val metricsService: MetricsService
) {

    fun getCurrentDateInt(): Int = timeRepository.findById(TIME_DAY_DB_ID)
        .orElse(
            Time(id = TIME_DAY_DB_ID, date = 0)
        ).date

    fun setCurrentDate(day: Int): TimeDto {
        if (day < getCurrentDateInt()) {
            throw InvalidBodyException("You can't set the date to the past")
        }

        metricsService.updateMaxDate(newDate = day)

        return TimeDto(
            timeRepository.save(
                Time(id = TIME_DAY_DB_ID, date = day)
            ).date
        )
    }

    init {
        getCurrentDateInt()
    }

}