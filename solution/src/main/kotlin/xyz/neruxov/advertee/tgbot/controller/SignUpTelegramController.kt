package xyz.neruxov.advertee.tgbot.controller

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.CallbackQueryUpdate
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Controller
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.tgbot.guard.UserRegisterGuard
import xyz.neruxov.advertee.tgbot.util.extensions.getAdvertiser
import xyz.neruxov.advertee.tgbot.util.extensions.isRegistered
import xyz.neruxov.advertee.tgbot.util.extensions.mainMenu
import xyz.neruxov.advertee.tgbot.util.extensions.register
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Controller
class SignUpTelegramController(
    private val advertiserRepository: AdvertiserRepository
) {

    @CommandHandler.CallbackQuery(["main_menu"], guard = UserRegisterGuard::class)
    suspend fun handleMainMenu(user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val advertiserName = user.getAdvertiser().name
        editMessageText(update.callbackQuery.message!!.messageId) { "Ты в главном меню!\n\nТекущий аккаунт: $advertiserName" }
            .inlineKeyboardMarkup { mainMenu() }.send(user, bot)
    }

    @CommandHandler(["/start"])
    suspend fun handleStartCommand(user: User, bot: TelegramBot) {
        if (user.isRegistered()) {
            val advertiserName = user.getAdvertiser().name
            return message { "Привет, рад видеть тебя снова!\n\nТекущий аккаунт: $advertiserName" }
                .inlineKeyboardMarkup { mainMenu() }
                .send(user, bot)
        }

        message { "Привет! Ты попал в бота для рекламодателей. Чтобы я понимал, кто ты, напиши мне свой UUID или название \uD83D\uDC47" }.inlineKeyboardMarkup { "✍\uFE0F Зарегистрироваться" callback "signup_new" }
            .send(
                user, bot
            )

        bot.inputListener[user] = "signup_uuid"
    }

    @CommandHandler.CallbackQuery(["signup_new"])
    suspend fun handleSignUpNew(user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        editMessageText(update.callbackQuery.message!!.messageId) { "Понял, создаём новый аккаунт. Как хочешь называться?" }.send(
            user, bot
        )

        bot.inputListener[user] = "signup_name"
        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    @InputHandler(["signup_name"])
    suspend fun handleSignUpName(user: User, bot: TelegramBot, update: ProcessedUpdate) {
        if (update !is MessageUpdate) return

        val advertiser = Advertiser(id = UUID.randomUUID(), name = update.text)
        advertiserRepository.save(advertiser)

        message { "Хорошо, зарегистрировал как ${advertiser.name} (${advertiser.id})!" }.inlineKeyboardMarkup { mainMenu() }
            .send(user, bot)

        user.register(advertiserId = advertiser.id)
    }

    @InputHandler(["signup_uuid"])
    suspend fun handleSignUpUuid(user: User, bot: TelegramBot, update: ProcessedUpdate) {
        if (update !is MessageUpdate) return

        var advertiser: Advertiser?
        try {
            val uuid = UUID.fromString(update.text)

            advertiser = advertiserRepository.findById(uuid).orElse(null)
        } catch (e: IllegalArgumentException) {
            advertiser = withContext(Dispatchers.IO) {
                advertiserRepository.searchByName(update.text)
            }

            if (advertiser == null) {
                message { "Ты ввёл неправильный UUID, попробуй снова!" }.send(user, bot)
                bot.inputListener[user] = "signup_uuid"

                return
            }
        }

        message { "Хорошо, ты залогинился как ${advertiser?.name} (${advertiser?.id})!" }.inlineKeyboardMarkup { mainMenu() }
            .send(user, bot)

        user.register(advertiserId = advertiser!!.id)
    }

    @CommandHandler.CallbackQuery(["change_account"])
    suspend fun handleChangeAccount(user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        editMessageText(update.callbackQuery.message!!.messageId) { "Хорошо, напиши мне свой UUID или название \uD83D\uDC47" }
            .inlineKeyboardMarkup { "✍\uFE0F Зарегистрироваться" callback "signup_new" }
            .send(
                user, bot
            )

        bot.inputListener[user] = "signup_uuid"
        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

}