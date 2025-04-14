package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class PayloadTooLargeException(
    message: String
) : StatusErrorException(413, message)