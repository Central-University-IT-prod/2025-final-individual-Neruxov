package xyz.neruxov.advertee.data.review.repo

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.review.model.ReviewRequest
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Repository
interface ReviewRequestRepository : CrudRepository<ReviewRequest, UUID> {

    fun findByVerdict(verdict: Boolean?, pageable: Pageable): List<ReviewRequest>

    fun deleteAllByCampaignId(campaignId: UUID)

    fun countByVerdict(verdict: Boolean?): Long

}