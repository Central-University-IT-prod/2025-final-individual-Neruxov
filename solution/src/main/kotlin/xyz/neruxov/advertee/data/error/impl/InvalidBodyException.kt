package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

data class InvalidBodyException(val message_: String? = null) : StatusErrorException(400, message_ ?: "Invalid body")