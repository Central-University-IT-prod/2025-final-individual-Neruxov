package xyz.neruxov.advertee.data.advertiser.repo

import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.advertiser.model.Advertiser
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Repository
interface AdvertiserRepository : CrudRepository<Advertiser, UUID> {

    @Cacheable("advertisers", key = "#id")
    override fun findById(id: UUID): Optional<Advertiser>

    @CachePut("advertisers", key = "#entity.id")
    override fun <S : Advertiser?> save(entity: S & Any): S & Any

    @Query("SELECT a FROM Advertiser a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun searchByName(name: String): Advertiser?

}