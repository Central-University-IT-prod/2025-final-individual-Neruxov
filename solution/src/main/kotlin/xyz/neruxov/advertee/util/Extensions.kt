package xyz.neruxov.advertee.util

import xyz.neruxov.advertee.data.ad.model.AdAction
import java.util.*

val ZERO_UUID = UUID(0, 0)

fun List<AdAction>.countByType(type: AdAction.Type): Int {
    return this.count { it.id.type == type }
}
