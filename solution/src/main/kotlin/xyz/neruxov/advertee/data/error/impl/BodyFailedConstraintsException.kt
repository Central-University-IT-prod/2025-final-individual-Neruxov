package xyz.neruxov.advertee.data.error.impl

import jakarta.validation.ConstraintViolation
import xyz.neruxov.advertee.data.error.StatusErrorException

data class BodyFailedConstraintsException(val violations: Collection<ConstraintViolation<*>>) :
    StatusErrorException(400, violations.joinToString { it.message })