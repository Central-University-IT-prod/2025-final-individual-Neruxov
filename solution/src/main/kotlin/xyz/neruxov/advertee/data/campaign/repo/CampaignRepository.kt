package xyz.neruxov.advertee.data.campaign.repo

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Limit
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.moderation.enum.ModerationStatus
import xyz.neruxov.advertee.util.enum.GenderFilter
import java.util.*


/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Repository
interface CampaignRepository : CrudRepository<Campaign, UUID> {

    @Cacheable("campaigns", key = "#id")
    override fun findById(id: UUID): Optional<Campaign>

    @Query(
        """
            SELECT c
            FROM Campaign c
            LEFT JOIN (
                SELECT a.campaign.id AS campaignId, COUNT(a) AS impressionsCount
                FROM AdAction a
                WHERE a.id.type = 'IMPRESSION'
                GROUP BY a.campaign.id
            ) a ON c.id = a.campaignId
            WHERE c.startDate <= :currentDate
              AND c.endDate >= :currentDate
              AND (
                    c.targeting.gender IS NULL OR c.targeting.gender = 'ALL' OR c.targeting.gender = :clientGender
                  )
              AND (
                    c.targeting.ageFrom IS NULL OR c.targeting.ageFrom <= :clientAge
                  )
              AND (
                    c.targeting.ageTo IS NULL OR c.targeting.ageTo >= :clientAge
                  )
              AND (
                    c.targeting.location IS NULL OR c.targeting.location = :clientLocation
                  )
              AND (
                    c.impressionsLimit != 0 AND
                    (COALESCE(a.impressionsCount, 0) + 1) / CAST(c.impressionsLimit as FLOAT) < 1.05
                  )
        """
    )
    fun findEligibleCampaigns(
        @Param("currentDate") currentDate: Int,
        @Param("clientGender") clientGender: GenderFilter,
        @Param("clientAge") clientAge: Int,
        @Param("clientLocation") clientLocation: String
    ): List<Campaign>

    // вот это я кэшировать точно не буду подождешь
    fun findAllByAdvertiserId(advertiserId: UUID, pageable: Pageable): List<Campaign>

    // тут тоже обойдешься, хотя может это зря.... лан
    fun findAllByAdvertiserId(advertiserId: UUID): List<Campaign>

    fun countByAdvertiserId(advertiserId: UUID): Long
    
    // правильно догадался
    fun findByModerationStatus(status: ModerationStatus, limit: Limit): List<Campaign>

    @CacheEvict("campaigns", key = "#entity.id")
    override fun delete(entity: Campaign)

    @CachePut("campaigns", key = "#entity.id")
    override fun <S : Campaign?> save(entity: S & Any): S & Any

    @Modifying
    @Query(
        """
            UPDATE Campaign c
            SET c.moderationStatus = :moderationStatus
            WHERE c.id = :id
        """
    )
    @CacheEvict("campaigns", key = "#id")
    fun updateModerationStatus(id: UUID, moderationStatus: ModerationStatus)

    fun existsByAttachmentId(attachmentId: UUID): Boolean

    @Query(
        """
            SELECT COALESCE(COUNT(c), 0)
            FROM Campaign c
            WHERE c.startDate <= :currentDate
              AND c.endDate >= :currentDate
        """
    )
    fun countActive(currentDate: Int): Int

    //    @Query("SELECT MAX (costPerImpression) FROM Campaign ")
    @Query("SELECT PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY costPerImpression) FROM Campaign")
    fun getCostPerImpression99thPercentile(): Float?

    //    @Query("SELECT MIN(costPerImpression) FROM Campaign")
    @Query("SELECT PERCENTILE_CONT(0.01) WITHIN GROUP (ORDER BY costPerImpression) FROM Campaign")
    fun getCostPerImpression1stPercentile(): Float?

    //    @Query("SELECT MAX(costPerClick) FROM Campaign ")
    @Query("SELECT PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY costPerClick) FROM Campaign")
    fun getCostPerClick99thPercentile(): Float?

    //    @Query("SELECT MIN(costPerClick) FROM Campaign")
    @Query("SELECT PERCENTILE_CONT(0.01) WITHIN GROUP (ORDER BY costPerClick) FROM Campaign")
    fun getCostPerClick1stPercentile(): Float?

}