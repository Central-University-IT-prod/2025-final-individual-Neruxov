package xyz.neruxov.advertee.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.Media
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Service

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class AIService(
    private val chatModel: OpenAiChatModel,
    private val objectMapper: ObjectMapper
) {

    fun call(
        systemPrompt: String,
        userPrompt: String,
        responseFormat: ResponseFormat = ResponseFormat.builder().type(ResponseFormat.Type.TEXT).build()
    ): String {
        return call(
            SystemMessage(systemPrompt),
            UserMessage(userPrompt),
            responseFormat
        )
    }

    fun callMedia(
        systemPrompt: String,
        media: Media,
        responseFormat: ResponseFormat = ResponseFormat.builder().type(ResponseFormat.Type.TEXT).build()
    ): String {
        return call(
            SystemMessage(systemPrompt), UserMessage("", media), responseFormat
        )
    }

    fun <T> callMediaJson(
        systemPrompt: String,
        media: Media,
        jsonSchema: String,
        targetClass: Class<T>
    ): T {
        return objectMapper.readValue(
            callMedia(
                systemPrompt, media, ResponseFormat(
                    ResponseFormat.Type.JSON_SCHEMA, jsonSchema
                )
            ),
            targetClass
        )
    }

    fun <T> callJson(systemPrompt: String, userPrompt: String, jsonSchema: String, targetClass: Class<T>): T {
        return objectMapper.readValue(
            call(
                systemPrompt, userPrompt, ResponseFormat(
                    ResponseFormat.Type.JSON_SCHEMA, jsonSchema
                )
            ),
            targetClass
        )
    }

    private fun call(
        systemMessage: SystemMessage,
        userMessage: UserMessage,
        responseFormat: ResponseFormat = ResponseFormat.builder().type(ResponseFormat.Type.TEXT).build()
    ): String {
        return chatModel.call(
            Prompt(
                listOf(
                    systemMessage,
                    userMessage
                ),
                OpenAiChatOptions.builder().model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                    .maxTokens(128) // деньги превыше всего
                    .responseFormat(responseFormat).build()
            ),
        ).result.output.text
    }

}