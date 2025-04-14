package xyz.neruxov.advertee.data.stats.response

import com.fasterxml.jackson.annotation.JsonProperty

data class StatsResponse(

    @JsonProperty("impresions_count")
    val impressionsCount: Int,

    @JsonProperty("clicks_count")
    val clicksCount: Int,

    val conversion: Float,

    @JsonProperty("spent_impressions")
    val spentImpressions: Float,

    @JsonProperty("spent_clicks")
    val spentClicks: Float,

    @JsonProperty("spent_total")
    val spentTotal: Float

) {

    fun toDailyStatsResponse(date: Int): DailyStatsResponse = DailyStatsResponse(
        impressionsCount = impressionsCount,
        clicksCount = clicksCount,
        conversion = conversion,
        spentImpressions = spentImpressions,
        spentClicks = spentClicks,
        spentTotal = spentTotal,
        date = date
    )

}