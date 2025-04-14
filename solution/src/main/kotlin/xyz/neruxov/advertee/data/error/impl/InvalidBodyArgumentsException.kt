package xyz.neruxov.advertee.data.error.impl

import org.springframework.validation.FieldError
import xyz.neruxov.advertee.data.error.StatusErrorException

data class InvalidBodyArgumentsException(val errors: Collection<FieldError>) :
    StatusErrorException(400, errors.joinToString(", ") { it.field + " " + it.defaultMessage!! })