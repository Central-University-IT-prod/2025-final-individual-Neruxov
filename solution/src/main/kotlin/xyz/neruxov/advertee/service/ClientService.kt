package xyz.neruxov.advertee.service

import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.client.model.Client
import xyz.neruxov.advertee.data.client.repo.ClientRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import java.util.*
import kotlin.jvm.optionals.getOrElse

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class ClientService(
    val clientRepository: ClientRepository
) {

    fun getById(id: UUID): Client {
        return clientRepository.findById(id)
            .getOrElse { throw NotFoundException("Client with id $id not found") }
    }

    fun create(client: Client): Client {
        return clientRepository.save(client)
    }

}