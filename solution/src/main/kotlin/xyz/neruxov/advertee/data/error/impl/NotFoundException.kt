package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
class NotFoundException(
    message: String
) : StatusErrorException(404, message)