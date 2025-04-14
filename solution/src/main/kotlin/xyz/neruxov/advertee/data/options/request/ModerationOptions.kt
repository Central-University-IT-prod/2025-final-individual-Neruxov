package xyz.neruxov.advertee.data.options.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ModerationOptions(
    @JsonProperty("image_enabled")
    val imageEnabled: Boolean,

    @JsonProperty("text_enabled")
    val textEnabled: Boolean
)
