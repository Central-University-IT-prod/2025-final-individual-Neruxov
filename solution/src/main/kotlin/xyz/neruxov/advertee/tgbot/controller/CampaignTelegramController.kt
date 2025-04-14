package xyz.neruxov.advertee.tgbot.controller

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.api.message.sendMessage
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.CallbackQueryUpdate
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import eu.vendeli.tgbot.utils.setChain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.service.CampaignService
import xyz.neruxov.advertee.service.StatsService
import xyz.neruxov.advertee.service.TimeService
import xyz.neruxov.advertee.tgbot.chain.CampaignCreateChain
import xyz.neruxov.advertee.tgbot.guard.UserRegisterGuard
import xyz.neruxov.advertee.tgbot.util.CampaignEditableFields
import xyz.neruxov.advertee.tgbot.util.EmojiUtility
import xyz.neruxov.advertee.tgbot.util.extensions.backTo
import xyz.neruxov.advertee.tgbot.util.extensions.displayPaged
import xyz.neruxov.advertee.tgbot.util.extensions.getUser
import xyz.neruxov.advertee.tgbot.util.extensions.removeKeyboard
import xyz.neruxov.advertee.util.enum.GenderFilter
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Controller
class CampaignTelegramController(
    private val campaignRepository: CampaignRepository,
    private val statsService: StatsService,
    private val campaignService: CampaignService,
    private val timeService: TimeService
) {

    private val editInfo = mutableMapOf<User, Pair<Campaign, CampaignEditableFields>>()
    private final val allowedNull =
        listOf(CampaignEditableFields.AGE_TO, CampaignEditableFields.AGE_FROM, CampaignEditableFields.LOCATION)

    private final val pageSize = 5

    @CommandHandler.CallbackQuery(["my_campaigns"], guard = UserRegisterGuard::class)
    suspend fun handleMyCampaigns(page: Int, user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val advertiserId = user.getUser().advertiserId

        val pageable = Pageable.ofSize(pageSize).withPage(page)
        val totalPages = max(withContext(Dispatchers.IO) {
            campaignRepository.countByAdvertiserId(advertiserId) + pageSize - 1
        } / pageSize, 1)

        val campaigns = withContext(Dispatchers.IO) {
            campaignRepository.findAllByAdvertiserId(advertiserId, pageable)
        }

        editMessageText(update.callbackQuery.message!!.messageId) { "Список твоих кампаний (${page + 1}/$totalPages):" }.inlineKeyboardMarkup {
            displayPaged(
                campaigns,
                { campaign ->
                    val emoji =
                        if (campaign.startDate <= timeService.getCurrentDateInt() && campaign.endDate >= timeService.getCurrentDateInt()) {
                            "\uD83D\uDFE2"
                        } else {
                            "\uD83D\uDD34"
                        }

                    "$emoji ${campaign.adTitle} (${campaign.id.toString().take(8)}...)"
                },
                { campaign -> "campaign?id=${campaign.id}" },
                { page -> "my_campaigns?page=$page" },
                page,
                totalPages.toInt()
            )

            newLine()
            "➕ Новая кампания" callback "create_campaign"

            newLine()
            backTo()
        }.send(user, bot)

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    @CommandHandler.CallbackQuery(["create_campaign"], guard = UserRegisterGuard::class)
    suspend fun handleCreateCampaign(user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        message { "Начинаем: какой ставим лимит на количество показов твоей кампании?" }.send(user, bot)

        bot.inputListener.setChain(user = user, firstLink = CampaignCreateChain.ImpressionLimit)
        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    @CommandHandler.CallbackQuery(["campaign"], guard = UserRegisterGuard::class)
    suspend fun handleCampaign(id: String, user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val campaignId = UUID.fromString(id)
        val campaignObject = withContext(Dispatchers.IO) {
            campaignRepository.findById(campaignId).orElseThrow()
        }

        val stats = statsService.getCampaignStats(campaignId)

        editMessageText(update.callbackQuery.message!!.messageId) {
            """
                ${campaignObject.adTitle}
                🛠 ID: ${campaignObject.id}
                
                📝 Текст: ${campaignObject.adText}
                
                📅 Сроки: ${campaignObject.startDate} - ${campaignObject.endDate} (${campaignObject.endDate - campaignObject.startDate} дней)
                
                👁 Показов: ${stats.impressionsCount}/${campaignObject.impressionsLimit}
                👆 Кликов: ${stats.clicksCount}/${campaignObject.clicksLimit}
                
                💸 Потрачено денег:
                 - Показы: ${String.format("%.2f", stats.spentImpressions)} ₽ (текущая цена: ${
                String.format(
                    "%.2f", campaignObject.costPerImpression
                )
            } ₽/показ)
                 - Клики: ${String.format("%.2f", stats.spentClicks)} ₽ (текущая цена: ${
                String.format(
                    "%.2f", campaignObject.costPerClick
                )
            } ₽/клик)
                Всего: ${String.format("%.2f", stats.spentTotal)} ₽
             
                ➡️ Конверсия (CTR): ${String.format("%.2f", stats.conversion)}%
            
                🚻 Таргетинг: 
                - Пол: ${getGenderFilter(campaignObject.targeting.gender ?: GenderFilter.ALL)}
                - Возраст: ${campaignObject.targeting.ageFrom ?: "любой"} - ${campaignObject.targeting.ageTo ?: "любой"}
                - Локация: ${campaignObject.targeting.location ?: "любая"}
                
                🗣 Статус модерации: ${getModerationStatus(campaignObject.moderationStatus.name)}
            """.trimIndent()
        }.inlineKeyboardMarkup {
            "\uD83D\uDCC8 Статистика по дням" callback "daily_stats?id=$id&date=0"
            newLine()
            "\uD83D\uDDD1 Удалить" callback "delete_campaign?id=$id&c=false"
            "✏\uFE0F Изменить" callback "edit_campaign?id=$id"
            newLine()
            backTo("my_campaigns?page=0")
        }.send(user, bot)

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    @CommandHandler.CallbackQuery(["edit_campaign"], guard = UserRegisterGuard::class)
    suspend fun handleEditCampaign(id: String, user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val campaignId = UUID.fromString(id)
        val campaignObject = withContext(Dispatchers.IO) {
            campaignRepository.findById(campaignId).orElseThrow()
        }

        val campaignStarted = campaignObject.startDate <= timeService.getCurrentDateInt()
        val editableFields =
            CampaignEditableFields.entries.filter { if (campaignStarted) it.editableAfterStart else true }

        editMessageText(update.callbackQuery.message!!.messageId) { "Что именно ты хочешь изменить в кампании?" }.inlineKeyboardMarkup {
            var i = 0
            editableFields.forEach {
                it.displayName callback "ec?id=$id&f=${it.name}"
                i++

                if (i % 2 == 0) {
                    newLine()
                }
            }
            newLine()
            backTo("campaign?id=$id")
        }.send(user, bot)

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    // тупой тг со своим ограном в 64 символа, приходится такое делать ;)
    @CommandHandler.CallbackQuery(["ec"], guard = UserRegisterGuard::class)
    suspend fun handleEditCampaignField(
        id: String, f: String, user: User, bot: TelegramBot, update: CallbackQueryUpdate
    ) {
        val campaignId = UUID.fromString(id)
        val campaignObject = withContext(Dispatchers.IO) {
            campaignRepository.findById(campaignId).orElseThrow()
        }

        val fieldObject = CampaignEditableFields.valueOf(f)
        var currentValue = fieldObject.getter.invoke(campaignObject)

        if (fieldObject == CampaignEditableFields.GENDER) currentValue =
            getGenderFilter((currentValue ?: GenderFilter.ALL) as GenderFilter)

        deleteMessage(update.callbackQuery.message!!.messageId).send(user, bot)

        sendMessage { "Текущее значение:\n${fieldObject.displayName}: ${currentValue ?: "не задано"}\n\nНа что ты хочешь его поменять?" }
            .replyKeyboardMarkup {
                if (fieldObject == CampaignEditableFields.GENDER) {
                    genderFilters.values.forEach {
                        +it
                    }

                    return@replyKeyboardMarkup
                }

                if (allowedNull.contains(fieldObject)) {
                    +"Отключить"
                }
            }.send(
                user, bot
            )

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)

        editInfo[user] = campaignObject to fieldObject
        bot.inputListener[user] = "edit_campaign_field"
    }

    @InputHandler(["edit_campaign_field"])
    suspend fun handleEditCampaignFieldInput(user: User, bot: TelegramBot, update: ProcessedUpdate) {
        if (update !is MessageUpdate) return

        val (campaignObject, fieldObject) = editInfo[user] ?: return
        val newValue = update.text

        val newValueCorrectType: Any?

        if (allowedNull.contains(fieldObject) && newValue == "Отключить") {
            newValueCorrectType = null
        } else {
            try {
                newValueCorrectType = when (fieldObject) {
                    CampaignEditableFields.IMPRESSIONS_LIMIT, CampaignEditableFields.CLICKS_LIMIT, CampaignEditableFields.AGE_FROM, CampaignEditableFields.AGE_TO, CampaignEditableFields.END_DATE, CampaignEditableFields.START_DATE -> {
                        val v =
                            newValue.toIntOrNull() ?: throw IllegalArgumentException("Мне кажется, что это не число.")

                        var strict = true
                        if (fieldObject == CampaignEditableFields.AGE_TO) {
                            strict = false
                            assert(
                                v >= (campaignObject.targeting.ageFrom ?: 0)
                            ) { "Максимальный возраст должен быть больше минимального." }
                        }

                        if (fieldObject == CampaignEditableFields.AGE_FROM) {
                            strict = false
                            assert(
                                v <= (campaignObject.targeting.ageTo ?: Int.MAX_VALUE)
                            ) { "Минимальный возраст должен быть меньше максимального." }
                        }

                        println("$fieldObject $v ${campaignObject.startDate}")
                        if (fieldObject == CampaignEditableFields.END_DATE) {
                            strict = false
                            println(v > campaignObject.startDate)
                            assert(v >= campaignObject.startDate) { "Дата окончания должна быть больше даты начала." }
                        }

                        if (fieldObject == CampaignEditableFields.START_DATE) {
                            strict = false
                            assert(v <= campaignObject.endDate) { "Дата начала должна быть меньше даты окончания." }
                            assert(v >= timeService.getCurrentDateInt()) { "Дата начала должна быть больше или равна текущей дате." }
                        }

                        assert(if (strict) v > 0 else v >= 0) { "Значение должно быть больше 0." }
                        v
                    }

                    CampaignEditableFields.COST_PER_IMPRESSION, CampaignEditableFields.COST_PER_CLICK -> {
                        val v =
                            newValue.toFloatOrNull() ?: throw IllegalArgumentException("Мне кажется, что это не число.")

                        assert(v > 0) { "Значение должно быть больше 0." }
                        v
                    }

                    CampaignEditableFields.AD_TITLE, CampaignEditableFields.AD_TEXT, CampaignEditableFields.LOCATION -> newValue

                    CampaignEditableFields.GENDER -> {
                        val v = getByGenderValue(newValue) ?: throw IllegalArgumentException("Такого варианта нет.")
                        v
                    }

                    else -> throw IllegalArgumentException("Неизвестное поле ;-;")
                }
            } catch (e: Exception) {
                message { "${e.message} Попробуй еще раз!" }.send(user, bot)
                bot.inputListener[user] = "edit_campaign_field"

                return
            }
        }

        val updatedCampaign = fieldObject.updater.invoke(campaignObject, newValueCorrectType)
        campaignRepository.save(updatedCampaign)

        editInfo.remove(user)

        user.removeKeyboard(bot)
        message { "Обновил!" }.inlineKeyboardMarkup { backTo("campaign?id=${campaignObject.id}") }.send(user, bot)
    }

    @CommandHandler.CallbackQuery(["delete_campaign"], guard = UserRegisterGuard::class)
    suspend fun handleDeleteCampaign(id: String, c: String, user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val campaignId = UUID.fromString(id)

        val confirm = c.toBoolean()
        if (!confirm) {
            editMessageText(update.callbackQuery.message!!.messageId) { "Ты уверен, что хочешь удалить кампанию?" }.inlineKeyboardMarkup {
                "✅ Да" callback "delete_campaign?id=$id&c=true"
                "❌ Нет" callback "campaign?id=$id"
            }.send(user, bot)

            answerCallbackQuery(update.callbackQuery.id)
            return
        }

        withContext(Dispatchers.IO) {
            campaignService.delete(user.getUser().advertiserId, campaignId)
        }

        editMessageText(update.callbackQuery.message!!.messageId) { "✅ Кампания удалена!" }.inlineKeyboardMarkup {
            backTo(
                "my_campaigns?page=0"
            )
        }.send(user, bot)

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    @CommandHandler.CallbackQuery(["daily_stats"], guard = UserRegisterGuard::class)
    suspend fun handleDailyStats(date: Int, id: String, user: User, bot: TelegramBot, update: CallbackQueryUpdate) {
        val pageSize = 4

        val campaignId = UUID.fromString(id)

        val upperLimit = min(date + pageSize, timeService.getCurrentDateInt() + 1) - 1
        val stats = statsService.getCampaignStatsDaily(campaignId)
            .subList(date, upperLimit + 1)

        editMessageText(update.callbackQuery.message!!.messageId) {
            """
Дни ${EmojiUtility.getCountEmoji(date)} - ${EmojiUtility.getCountEmoji(upperLimit)} (сейчас день ${
                EmojiUtility.getCountEmoji(
                    timeService.getCurrentDateInt()
                )
            }):

${
                stats.joinToString("\n\n") {
                    """
День ${EmojiUtility.getCountEmoji(it.date)}.
👁 ${it.impressionsCount} показов (потрачено: ${String.format("%.2f", it.spentImpressions)} ₽)
🖱 ${it.clicksCount} кликов (потрачено: ${String.format("%.2f", it.spentClicks)} ₽)
➡️ CTR: ${String.format("%.2f", it.conversion)}%
""".trimIndent()
                }
            }
            """.trimIndent()
        }.inlineKeyboardMarkup {
            if (date > 0) {
                "⬅\uFE0F" callback "daily_stats?id=$id&date=${max(date - pageSize, 0)}"
            }

            if (date + pageSize < timeService.getCurrentDateInt()) {
                "➡\uFE0F" callback "daily_stats?id=$id&date=${upperLimit + 1}"
            }

            newLine()
            backTo("campaign?id=$id")
        }.send(user, bot)

        answerCallbackQuery(update.callbackQuery.id).send(user, bot)
    }

    private fun getModerationStatus(status: String) = when (status) {
        "UNMODERATED" -> "Не требуется"
        "AWAITING_MODERATION" -> "Ожидает проверки"
        "APPROVED" -> "Одобрено"
        "REJECTED" -> "Отклонено"
        else -> "Неизвестно"
    }

    private val genderFilters = mapOf(
        GenderFilter.MALE to "Только мужчины", GenderFilter.FEMALE to "Только женщины", GenderFilter.ALL to "Любой"
    )

    fun getGenderFilter(genderFilter: GenderFilter) = genderFilters[genderFilter]

    fun getByGenderValue(value: String) = genderFilters.entries.firstOrNull { it.value == value }?.key

    private fun assert(condition: Boolean, message: () -> String) {
        if (!condition) {
            throw IllegalArgumentException(message())
        }
    }

}