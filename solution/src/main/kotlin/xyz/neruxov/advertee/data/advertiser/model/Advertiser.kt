package xyz.neruxov.advertee.data.advertiser.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty
import java.util.*

@Entity
@Table(name = "advertisers")
data class Advertiser(

    @Id
    @JsonProperty("advertiser_id")
    val id: UUID,

    @field:NotEmpty
    val name: String

)