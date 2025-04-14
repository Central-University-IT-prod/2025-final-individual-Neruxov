package xyz.neruxov.advertee.data.moderation.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ImageModerationResult @JsonCreator constructor(
    @JsonProperty("safe") val safe: Boolean,
    @JsonProperty("reason") val reason: String?
)
