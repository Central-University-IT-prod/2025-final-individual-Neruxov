package xyz.neruxov.advertee.data.client.repo

import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.client.model.Client
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Repository
interface ClientRepository : CrudRepository<Client, UUID> {

    @Cacheable("clients", key = "#id")
    override fun findById(id: UUID): Optional<Client>

    @CachePut("clients", key = "#entity.id")
    override fun <S : Client?> save(entity: S & Any): S & Any

}