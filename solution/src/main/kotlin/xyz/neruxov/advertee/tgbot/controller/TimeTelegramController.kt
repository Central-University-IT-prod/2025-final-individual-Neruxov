package xyz.neruxov.advertee.tgbot.controller

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.CallbackQueryUpdate
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import org.springframework.stereotype.Controller
import xyz.neruxov.advertee.service.TimeService
import xyz.neruxov.advertee.tgbot.util.extensions.backTo

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Controller
class TimeTelegramController(private val timeService: TimeService) {

    @CommandHandler.CallbackQuery(["set_time"])
    suspend fun handleSetTime(user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        editMessageText(update.callbackQuery.message!!.messageId) { "Хорошо, какую ставим дату?\n\nТекущая дата: ${timeService.getCurrentDateInt()}" }
            .send(user, bot)

        bot.inputListener[user] = "set_time"
    }

    @InputHandler(["set_time"])
    suspend fun handleSetTimeInput(user: User, bot: TelegramBot, update: ProcessedUpdate) {
        if (update !is MessageUpdate) return

        val date = update.message.text?.toIntOrNull()
        if (date == null || date < timeService.getCurrentDateInt()) {
            message("Мне кажется, что это не число, или оно меньше текущей даты, попробуй еще раз!").send(user, bot)
            bot.inputListener[user] = "set_time"

            return
        }

        timeService.setCurrentDate(date)
        message("Изменил на $date!")
            .inlineKeyboardMarkup { backTo() }
            .send(user, bot)
    }

}