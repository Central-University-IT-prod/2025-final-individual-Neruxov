package xyz.neruxov.advertee.tgbot.guard

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.interfaces.helper.Guard
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import org.springframework.stereotype.Component
import xyz.neruxov.advertee.tgbot.util.extensions.isRegistered

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Component
class UserRegisterGuard : Guard {

    override suspend fun condition(user: User?, update: ProcessedUpdate, bot: TelegramBot) =
        user?.isRegistered() ?: false

}