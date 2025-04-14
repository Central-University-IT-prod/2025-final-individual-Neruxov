package xyz.neruxov.advertee.service

import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import java.util.*
import kotlin.jvm.optionals.getOrElse

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class AdvertiserService(
    val advertiserRepository: AdvertiserRepository
) {

    fun getById(id: UUID): Advertiser {
        return advertiserRepository.findById(id)
            .getOrElse { throw NotFoundException("Advertiser with id $id not found") }
    }

    fun create(advertiser: Advertiser): Advertiser {
        return advertiserRepository.save(advertiser)
    }

}