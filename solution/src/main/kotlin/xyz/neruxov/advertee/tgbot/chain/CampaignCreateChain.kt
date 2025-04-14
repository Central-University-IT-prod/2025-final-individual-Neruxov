package xyz.neruxov.advertee.tgbot.chain

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.InputChain
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.BreakCondition
import eu.vendeli.tgbot.types.internal.ChainLink
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import eu.vendeli.tgbot.types.internal.chain.BaseStatefulLink
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.request.CampaignCreateRequest
import xyz.neruxov.advertee.service.CampaignService
import xyz.neruxov.advertee.service.TimeService
import xyz.neruxov.advertee.tgbot.util.extensions.backTo
import xyz.neruxov.advertee.tgbot.util.extensions.getAdvertiser
import xyz.neruxov.advertee.tgbot.util.extensions.removeKeyboard
import xyz.neruxov.advertee.util.enum.GenderFilter

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Component
@InputChain
object CampaignCreateChain {

    // в либе не работают стейты со спрингом, отлично!
    // псих, который знает где я живу, если ты это читаешь - пожалуйста прости меня грешного
    private val states = mutableMapOf<User, MutableMap<Class<out BaseStatefulLink>, String>>()

    @Component
    object ImpressionLimit : BaseStatefulLink() {

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                update.text.toInt()
                false
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "По-моему это не число, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Хорошо, запомнил! Сколько ты готов платить за один показ?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object ImpressionCost : BaseStatefulLink() {

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                update.text.toFloat()
                false
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Не могу найти тут число, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Уже записал! Максимальное количество кликов?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object ClickLimit : BaseStatefulLink() {

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                update.text.toFloat()
                false
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Это не число :( Попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Учту! Сколько ты готов платить за один клик?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object ClickCost : BaseStatefulLink() {

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                update.text.toFloat()
                false
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Я не вижу здесь числа, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Записано! Какой будет заголовок?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object Title : BaseStatefulLink() {

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Отлично! Текст?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object Text : BaseStatefulLink() {

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Супер! Когда запускать?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }


    @Component
    object StartDate : BaseStatefulLink() {

        private lateinit var timeService: TimeService

        @Autowired
        fun initializeTimeService(ts: TimeService) {
            timeService = ts
        }

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                val startDate = update.text.toInt()

                startDate < timeService.getCurrentDateInt()
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Либо не число, либо кампания начинается в прошлом, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Ага! Когда останавливать?" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object EndDate : BaseStatefulLink() {

        override val breakCondition = BreakCondition { user, update, _ ->
            try {
                val endDate = update.text.toInt()

                val startDate = getState(user, StartDate)!!.toInt()

                startDate > endDate
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Или это не номер дня, или кампания начинается после ее окончания, попробуй еще раз!" }.send(
                user,
                bot
            )
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Nice! Какой будет минимум по возрасту?" }.replyKeyboardMarkup { +"Пропустить" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object AgeMinLimit : BaseStatefulLink() {

        override val breakCondition = BreakCondition { _, update, _ ->
            try {
                if (update.text == "Пропустить") return@BreakCondition false

                update.text.toInt()

                false
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Это не число, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Понял! Максимум по возрасту?" }.replyKeyboardMarkup { +"Пропустить" }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object AgeMaxLimit : BaseStatefulLink() {

        override val breakCondition = BreakCondition { user, update, _ ->
            try {
                if (update.text == "Пропустить") return@BreakCondition false

                val ageLimit = update.text.toInt()
                val ageMinLimit = getState(user, AgeMinLimit)!!.toInt()

                ageLimit < ageMinLimit
            } catch (e: NumberFormatException) {
                true
            }
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Это не число, или оно меньше минимального возраста, попробуй еще раз!" }.send(
                user, bot
            )
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "Договорились! Ограничение по полу?" }.replyKeyboardMarkup {
                +"Только мужчины"
                +"Только женщины"
                +"Любой"
            }.send(user, bot)

            saveState(user, this, update.text)
            return update.text
        }

    }

    @Component
    object GenderLimit : BaseStatefulLink() {

        private val values: Map<String, GenderFilter> = mapOf(
            "Только мужчины" to GenderFilter.MALE,
            "Только женщины" to GenderFilter.FEMALE,
            "Любой" to GenderFilter.ALL,
        )

        override val breakCondition = BreakCondition { _, update, _ ->
            update.text !in values.keys
        }

        override suspend fun breakAction(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            message { "Такого варианта нет, попробуй еще раз!" }.send(user, bot)
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot): String {
            message { "ОК! Какую локацию хочешь таргетировать?" }.replyKeyboardMarkup {
                +"Пропустить"
            }.send(user, bot)

            saveState(user, this, values[update.text]!!.name)
            return values[update.text]!!.name
        }

    }

    @Component
    object LocationLimit : ChainLink() {

        private lateinit var campaignService: CampaignService

        @Autowired
        fun initializeCampaignService(cs: CampaignService) {
            campaignService = cs
        }

        override suspend fun action(user: User, update: ProcessedUpdate, bot: TelegramBot) {
            if (update !is MessageUpdate) return

            val locationLimit = if (update.text == "Пропустить") null else update.text
            val ageFrom = if (getState(user, AgeMinLimit) == "Пропустить") 0 else getState(user, AgeMinLimit)!!.toInt()
            val ageTo = if (getState(user, AgeMaxLimit) == "Пропустить") 100 else getState(user, AgeMaxLimit)!!.toInt()

            val campaign = campaignService.create(
                user.getAdvertiser().id,
                CampaignCreateRequest(
                    impressionsLimit = getState(user, ImpressionLimit)!!.toInt(),
                    costPerImpression = getState(user, ImpressionCost)!!.toFloat(),
                    clicksLimit = getState(user, ClickLimit)!!.toInt(),
                    costPerClick = getState(user, ClickCost)!!.toFloat(),
                    adTitle = getState(user, Title)!!,
                    adText = getState(user, Text)!!,
                    startDate = getState(user, StartDate)!!.toInt(),
                    endDate = getState(user, EndDate)!!.toInt(),
                    targeting = Campaign.Targeting(
                        ageFrom = ageFrom,
                        ageTo = ageTo,
                        gender = GenderFilter.valueOf(getState(user, GenderLimit)!!),
                        location = locationLimit
                    )
                )
            )

            user.removeKeyboard(bot)

            message { "Всё, твоя кампания создана! (${campaign.id})" }
                .inlineKeyboardMarkup { backTo() }
                .send(user, bot)

            clearStates(user)
        }

    }

    private fun saveState(user: User, link: BaseStatefulLink, value: String) {
        states.getOrPut(user) { mutableMapOf() }[link::class.java] = value
    }

    private fun getState(user: User, link: BaseStatefulLink): String? {
        return states[user]?.get(link::class.java)
    }

    private fun clearStates(user: User) {
        states.remove(user)
    }

}