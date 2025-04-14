package xyz.neruxov.advertee.data.moderation.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class TextModerationResult @JsonCreator constructor(
    @JsonProperty("safe") val safe: Boolean,
    @JsonProperty("requires_manual_review") val requiresManualReview: Boolean,
    @JsonProperty("reason") val reason: String?
)
