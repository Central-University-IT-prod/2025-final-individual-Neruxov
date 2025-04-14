package xyz.neruxov.advertee.data.error

sealed interface StatusResponse {

    val statusCode: Int
    val status: String
    val message: String

    data class Ok(
        override val status: String = "ok",
        override val message: String = "ok",
        override val statusCode: Int = 200
    ) : StatusResponse

    data class Error(
        override val statusCode: Int = 400,
        override val message: String,
        override val status: String = "error",
    ) : StatusResponse

    fun asMap() = mapOf(
        "status" to status, "message" to message
    )

}

open class StatusErrorException(
    private val response: StatusResponse.Error
) : RuntimeException(response.message), StatusResponse by response {

    constructor(statusCode: Int, message: String) : this(
        StatusResponse.Error(
            message = message,
            statusCode = statusCode
        )
    )

    override val message: String get() = response.message

}
