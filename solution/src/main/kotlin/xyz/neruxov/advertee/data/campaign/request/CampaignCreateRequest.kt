package xyz.neruxov.advertee.data.campaign.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import xyz.neruxov.advertee.data.campaign.model.Campaign
import java.util.*

data class CampaignCreateRequest(

    @field:Min(0) @JsonProperty("impressions_limit") val impressionsLimit: Int,

    @field:Min(0) @JsonProperty("clicks_limit") val clicksLimit: Int,

    @field:Positive(message = "Cost per impression must be positive")
    @JsonProperty("cost_per_impression") val costPerImpression: Float,

    @field:Positive(message = "Cost per click must be positive")
    @JsonProperty("cost_per_click") val costPerClick: Float,

    @field:NotEmpty @JsonProperty("ad_title") val adTitle: String,

    @field:NotEmpty @JsonProperty("ad_text") val adText: String,

    @field:Min(0) @JsonProperty("start_date") val startDate: Int,

    @field:Min(0) @JsonProperty("end_date") val endDate: Int,

    @JsonProperty("attachment_id") val attachmentId: UUID? = null,

    @field:Valid
    val targeting: Campaign.Targeting?

)