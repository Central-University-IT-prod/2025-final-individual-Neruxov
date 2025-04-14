package xyz.neruxov.advertee.data.error.impl

import xyz.neruxov.advertee.data.error.StatusErrorException

data object EndpointNotFoundException : StatusErrorException(404, "Endpoint not found") {
    private fun readResolve(): Any = EndpointNotFoundException
}
