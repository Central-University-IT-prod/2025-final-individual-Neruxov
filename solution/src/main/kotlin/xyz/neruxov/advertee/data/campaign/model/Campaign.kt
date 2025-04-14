package xyz.neruxov.advertee.data.campaign.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import xyz.neruxov.advertee.data.ad.dto.Ad
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.attachment.model.Attachment
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.util.enum.GenderFilter
import java.util.*

@Entity
@Table(name = "campaigns")
data class Campaign(

    // увы он не работает без нуллабл
    @Id
    @JsonProperty("campaign_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", referencedColumnName = "id", insertable = false, updatable = false)
    val advertiser: Advertiser? = null,

    @JsonProperty("advertiser_id")
    @Column(name = "advertiser_id")
    val advertiserId: UUID,

    @field:Min(0)
    @JsonProperty("impressions_limit")
    val impressionsLimit: Int,

    @field:Min(0)
    @JsonProperty("clicks_limit")
    val clicksLimit: Int,

    @field:Positive(message = "Cost per impression must be positive")
    @JsonProperty("cost_per_impression")
    val costPerImpression: Float,

    @field:Positive(message = "Cost per click must be positive")
    @JsonProperty("cost_per_click")
    val costPerClick: Float,

    @field:NotEmpty
    @JsonProperty("ad_title")
    val adTitle: String,

    @field:NotEmpty
    @JsonProperty("ad_text")
    val adText: String,

    @field:Min(0)
    @JsonProperty("start_date")
    val startDate: Int,

    @field:Min(0)
    @JsonProperty("end_date")
    val endDate: Int,

    @Column(name = "attachment_id")
    @JsonProperty("attachment_id")
    val attachmentId: UUID?,

    @Embedded
    @field:Valid
    val targeting: Targeting = Targeting(),

    @Enumerated(EnumType.STRING)
    @JsonProperty("moderation_status")
    val moderationStatus: ModerationStatus = ModerationStatus.AWAITING_MODERATION,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", referencedColumnName = "id", insertable = false, updatable = false)
    val attachment: Attachment? = null,

) {

    @Embeddable
    data class Targeting(

        @Enumerated(EnumType.STRING)
        val gender: GenderFilter? = null,

        @field:Min(0)
        @JsonProperty("age_from")
        val ageFrom: Int? = null,

        @field:Min(0)
        @JsonProperty("age_to")
        val ageTo: Int? = null,

        @field:Size(min = 1)
        val location: String? = null

    )

    fun toAd(): Ad = Ad(
        id = id!!, title = adTitle, text = adText, attachmentId = attachmentId, advertiserId = advertiserId
    )

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Campaign) return false
        return id == other.id
    }

}
