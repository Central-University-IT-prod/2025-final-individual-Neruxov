package xyz.neruxov.advertee.data.mlscore.repo

import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.mlscore.model.MLScore
import java.util.*

@Repository
interface MLScoreRepository : CrudRepository<MLScore, MLScore.Id> {

    // CACHING NOT TESTED (TODO)
    @Cacheable("ml_scores", key = "#id")
    override fun findById(id: MLScore.Id): Optional<MLScore>

    @CachePut("ml_scores", key = "#entity.id")
    override fun <S : MLScore?> save(entity: S & Any): S & Any

    @Query("SELECT PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY score) FROM MLScore")
    fun get99thPercentile(): Int?

    @Query("SELECT PERCENTILE_CONT(0.01) WITHIN GROUP (ORDER BY score) FROM MLScore")
    fun get1stPercentile(): Int?

}