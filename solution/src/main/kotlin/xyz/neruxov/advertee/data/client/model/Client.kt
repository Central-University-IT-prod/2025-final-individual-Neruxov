package xyz.neruxov.advertee.data.client.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import xyz.neruxov.advertee.util.enum.Gender
import java.util.*

// да, мне лень разделять это на энтити и модель
@Entity
@Table(name = "clients")
data class Client(

    @Id
    @JsonProperty("client_id")
    val id: UUID,

    @field:NotEmpty
    val login: String,

    @field:Min(value = 0, message = "Age must be greater or equal to 0")
    val age: Int,

    @field:NotEmpty
    val location: String,

    @Enumerated(EnumType.STRING)
    val gender: Gender

)
