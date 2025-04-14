package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

data object InternalServerErrorException :
    StatusErrorException(500, "Something went wrong on our side, sorry!") {
    private fun readResolve(): Any = InternalServerErrorException
}