package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

data class UnsupportedMediaTypeException(val message_: String = "Unsupported media type") :
    StatusErrorException(415, message_)