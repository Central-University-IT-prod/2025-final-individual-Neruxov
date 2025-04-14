package xyz.neruxov.advertee.tgbot.util

import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.util.enum.GenderFilter

enum class CampaignEditableFields(
    val displayName: String,
    val editableAfterStart: Boolean = true,
    val updater: Campaign.(v: Any?) -> Campaign = { this },
    val getter: Campaign.() -> Any?
) {
    IMPRESSIONS_LIMIT(
        "\uD83D\uDC41 Лимит показов", false, { v -> copy(impressionsLimit = v as Int) }, { impressionsLimit }),
    CLICKS_LIMIT(
        "🖱️ Лимит кликов", false, { v -> copy(clicksLimit = v as Int) }, { clicksLimit }),
    COST_PER_IMPRESSION(
        "\uD83D\uDC41💰 Цена за показ",
        updater = { v -> copy(costPerImpression = v as Float) },
        getter = { costPerImpression }),
    COST_PER_CLICK(
        "🖱️💰 Цена за клик", updater = { v -> copy(costPerClick = v as Float) }, getter = { costPerClick }),
    AD_TITLE(
        "📝 Заголовок", updater = { v -> copy(adTitle = v as String) }, getter = { adTitle }),
    AD_TEXT(
        "📝 Текст", updater = { v -> copy(adText = v as String) }, getter = { adText }),
    START_DATE(
        "📅 Дата начала", false, { v -> copy(startDate = v as Int) }, getter = { startDate }),
    END_DATE(
        "📅 Дата окончания", false, { v -> copy(endDate = v as Int) }, getter = { endDate }),
    AGE_FROM(
        "🔞 Мин. возраст",
        updater = { v -> copy(targeting = targeting.copy(ageFrom = v as Int?)) },
        getter = { targeting.ageFrom }),
    AGE_TO(
        "🔞 Макс. возраст",
        updater = { v -> copy(targeting = targeting.copy(ageTo = v as Int?)) },
        getter = { targeting.ageTo }),
    GENDER(
        "🚻 Пол",
        updater = { v -> copy(targeting = targeting.copy(gender = v as GenderFilter?)) },
        getter = { targeting.gender }),
    LOCATION(
        "📍 Локация",
        updater = { v -> copy(targeting = targeting.copy(location = v as String?)) },
        getter = { targeting.location })
}