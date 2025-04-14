package xyz.neruxov.advertee.data.ad.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class Ad(

    @JsonProperty("ad_id")
    val id: UUID,

    @JsonProperty("ad_title")
    val title: String,

    @JsonProperty("ad_text")
    val text: String,

    @JsonProperty("attachment_id")
    val attachmentId: UUID?,

    @JsonProperty("advertiser_id")
    val advertiserId: UUID

)