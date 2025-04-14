package xyz.neruxov.advertee.tgbot.data.user.repo

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.tgbot.data.user.model.TelegramUser

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Repository
interface TelegramUserRepository : CrudRepository<TelegramUser, Long>