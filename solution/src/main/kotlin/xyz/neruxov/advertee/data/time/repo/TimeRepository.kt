package xyz.neruxov.advertee.data.time.repo

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.time.model.Time
import java.util.*

@Repository
interface TimeRepository : CrudRepository<Time, Int> {

    @Cacheable("days", key = "#id")
    override fun findById(id: Int): Optional<Time>

    @CacheEvict("days", key = "#entity.id")
    override fun <S : Time?> save(entity: S & Any): S & Any

}