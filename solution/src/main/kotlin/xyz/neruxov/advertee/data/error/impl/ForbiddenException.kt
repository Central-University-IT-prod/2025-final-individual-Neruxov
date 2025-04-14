package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
data class ForbiddenException(val message_: String? = null) : StatusErrorException(403, message_ ?: "Forbidden")