package xyz.neruxov.advertee.data.generation.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotEmpty
import java.util.*

data class AdContentGenerationRequest(

    @JsonProperty("advertiser_id")
    val advertiserId: UUID,

    @field:NotEmpty
    val request: String

)