package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

data object HttpMethodNotAllowedException : StatusErrorException(405, "Method not allowed") {
    private fun readResolve(): Any = HttpMethodNotAllowedException
}