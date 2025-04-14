package xyz.neruxov.advertee.data.attachment.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import java.util.*

@Entity
@Table(name = "attachments")
data class Attachment(

    @Id
    @GeneratedValue
    val id: UUID? = null,

    @JsonProperty("advertiser_id")
    @Column(name = "advertiser_id")
    val advertiserId: UUID,

    val name: String,

    val extension: String?,

    @JsonProperty("content_type")
    val contentType: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", referencedColumnName = "id", insertable = false, updatable = false)
    val advertiser: Advertiser? = null

)
