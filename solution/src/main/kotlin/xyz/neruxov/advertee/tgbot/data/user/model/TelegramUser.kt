package xyz.neruxov.advertee.tgbot.data.user.model

import jakarta.persistence.*
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import java.util.*

@Entity
@Table(name = "telegram_users")
data class TelegramUser(

    @Id
    val id: Long,

    @Column(name = "advertiser_id")
    val advertiserId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", referencedColumnName = "id", insertable = false, updatable = false)
    val advertiser: Advertiser? = null

)