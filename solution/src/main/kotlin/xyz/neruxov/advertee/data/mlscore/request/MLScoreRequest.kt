package xyz.neruxov.advertee.data.mlscore.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import java.util.*

data class MLScoreRequest(

    @JsonProperty("client_id")
    val clientId: UUID,

    @JsonProperty("advertiser_id")
    val advertiserId: UUID,

    @field:Min(0)
    val score: Int

)
