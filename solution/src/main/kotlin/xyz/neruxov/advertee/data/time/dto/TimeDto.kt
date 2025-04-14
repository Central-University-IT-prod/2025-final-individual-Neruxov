package xyz.neruxov.advertee.data.time.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min

data class TimeDto(

    @field:Min(0)
    @JsonProperty("current_date")
    val currentDate: Int

)