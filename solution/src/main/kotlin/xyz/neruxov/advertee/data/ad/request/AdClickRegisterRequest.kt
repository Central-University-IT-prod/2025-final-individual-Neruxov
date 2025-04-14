package xyz.neruxov.advertee.data.ad.request

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class AdClickRegisterRequest(

    @JsonProperty("client_id")
    val clientId: UUID

)
