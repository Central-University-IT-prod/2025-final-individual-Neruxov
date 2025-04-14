package xyz.neruxov.advertee.data.ad.model

import jakarta.persistence.*
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.client.model.Client
import java.util.*

@Entity
@Table(name = "actions")
data class AdAction(

    @EmbeddedId
    val id: Id,

    val cost: Float,

    val date: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", referencedColumnName = "id", insertable = false, updatable = false)
    val campaign: Campaign? = null, // для релейшенов

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", insertable = false, updatable = false)
    val client: Client? = null, // для релейшенов

) {

    enum class Type {
        IMPRESSION, CLICK
    }

    @Embeddable
    data class Id(

        @Column(name = "campaign_id")
        val campaignId: UUID,

        @Column(name = "client_id")
        val clientId: UUID,

        @Enumerated(EnumType.STRING)
        val type: Type

    )

}