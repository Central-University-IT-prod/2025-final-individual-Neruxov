package xyz.neruxov.advertee.data.mlscore.model

import jakarta.persistence.*
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.client.model.Client
import java.util.*

@Entity
@Table(name = "ml_scores")
data class MLScore(

    @EmbeddedId
    val id: Id,

    val score: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", referencedColumnName = "id", insertable = false, updatable = false)
    val advertiser: Advertiser? = null, // для релейшенов

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", insertable = false, updatable = false)
    val client: Client? = null, // для релейшенов

) {

    @Embeddable
    data class Id(

        @Column(name = "advertiser_id")
        val advertiserId: UUID,

        @Column(name = "client_id")
        val clientId: UUID

    )

}
