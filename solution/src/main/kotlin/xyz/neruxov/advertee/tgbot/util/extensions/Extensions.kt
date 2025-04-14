package xyz.neruxov.advertee.tgbot.util.extensions

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.SendMessageAction
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.tgbot.data.user.model.TelegramUser
import xyz.neruxov.advertee.tgbot.data.user.repo.TelegramUserRepository
import java.util.*

/*
 * я гений, если что, вопросов не задавайте пожалуйста
 */
@Component
object RepositoryUtil {

    lateinit var telegramUserRepository: TelegramUserRepository

    lateinit var advertiserRepository: AdvertiserRepository

    @Autowired
    fun initializeUserRepository(ur: TelegramUserRepository) {
        telegramUserRepository = ur
    }

    @Autowired
    fun initializeAdvertiserRepository(ar: AdvertiserRepository) {
        advertiserRepository = ar
    }

}

suspend fun User.getUser(): TelegramUser = withContext(Dispatchers.IO) {
    RepositoryUtil.telegramUserRepository.findById(id).orElseThrow()
}

suspend fun User.getAdvertiser(): Advertiser = withContext(Dispatchers.IO) {
    RepositoryUtil.advertiserRepository.findById(getUser().advertiserId).orElseThrow()
}

suspend fun User.isRegistered() = withContext(Dispatchers.IO) {
    RepositoryUtil.telegramUserRepository.existsById(id)
}

suspend fun User.register(advertiserId: UUID) = withContext(Dispatchers.IO) {
    RepositoryUtil.telegramUserRepository.save(TelegramUser(id, advertiserId))
}

suspend fun User.removeKeyboard(bot: TelegramBot) {
    val message = message { "\uD83E\uDD2A" }
        .replyKeyboardRemove()
        .sendAndDelete(this, bot)
}

suspend fun SendMessageAction.sendAndDelete(user: User, bot: TelegramBot) {
    val message = sendReturning(user, bot).getOrNull()
    deleteMessage(messageId = message?.messageId ?: return).send(user, bot)
}
