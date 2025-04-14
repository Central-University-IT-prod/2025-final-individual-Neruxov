package xyz.neruxov.advertee.data.review.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import xyz.neruxov.advertee.data.campaign.model.Campaign
import java.util.*

@Entity
@Table(name = "review_requests")
data class ReviewRequest(

    @Id
    @GeneratedValue
    @JsonProperty("review_request_id")
    val id: UUID? = null,

    @JsonIgnore
    @Column(name = "campaign_id")
    val campaignId: UUID,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", referencedColumnName = "id", insertable = false, updatable = false)
    val campaign: Campaign? = null,

    val adTitle: String,

    val adText: String,

    val aiReason: String,

    @JsonIgnore
    val verdict: Boolean? = null

)