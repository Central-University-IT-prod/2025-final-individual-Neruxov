package xyz.neruxov.advertee.data.ad.repo

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.ad.model.AdAction
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 * TODO: Caching...?
 */
@Repository
interface AdActionRepository : CrudRepository<AdAction, AdAction.Id> {

    fun findAllByCampaignId(campaignId: UUID): List<AdAction>

    fun findAllByCampaignIdIn(campaignIds: List<UUID>): List<AdAction>

    fun deleteAllByCampaignId(campaignId: UUID)

    @Query(
        """
            SELECT COALESCE(SUM(a.cost), 0)
            FROM AdAction a
        """
    )
    fun getTotalCost(): Float

    fun findAllBy(limit: Limit): List<AdAction>

}