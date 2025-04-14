package xyz.neruxov.advertee.data.time.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import xyz.neruxov.advertee.service.TIME_DAY_DB_ID

@Entity
@Table(name = "time")
data class Time(

    @Id
    @JsonIgnore
    val id: Int = TIME_DAY_DB_ID,

    val date: Int

)
