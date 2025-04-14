package xyz.neruxov.advertee.tgbot.util.extensions

import eu.vendeli.tgbot.utils.builders.InlineKeyboardMarkupBuilder

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
fun InlineKeyboardMarkupBuilder.mainMenu() {
    "\uD83D\uDCC3 Мои кампании" callback "my_campaigns?page=0"
    newLine()
    "➕ Новая кампания" callback "create_campaign"
    newLine()
    "\uD83D\uDC64 Сменить аккаунт" callback "change_account"
    newLine()
    "\uD83D\uDCC6 Сменить дату" callback "set_time"
}

fun InlineKeyboardMarkupBuilder.backTo(query: String = "main_menu") {
    "⬅️ Назад" callback query
}

fun <T> InlineKeyboardMarkupBuilder.displayPaged(
    data: List<T>,
    nameSupplier: (T) -> String,
    callbackSupplier: (T) -> String,
    pageCallbackSupplier: (Int) -> String,
    page: Int,
    totalPages: Int,
    pageSize: Int = 5
) {
    data.forEach {
        val name = nameSupplier(it)
        val callback = callbackSupplier(it)

        name callback callback
        newLine()
    }

    if (data.isEmpty()) {
        "Тут пусто :(" callback "what_are_you_looking_at"
    }

    if (page > 0) {
        "⬅️" callback pageCallbackSupplier(page - 1)
    }

    if (page < totalPages - 1) {
        "➡️" callback pageCallbackSupplier(page + 1)
    }
}