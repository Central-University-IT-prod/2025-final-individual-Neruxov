package xyz.neruxov.advertee.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.ad.dto.Ad
import xyz.neruxov.advertee.data.ad.model.AdAction
import xyz.neruxov.advertee.data.ad.repo.AdActionRepository
import xyz.neruxov.advertee.data.ad.request.AdClickRegisterRequest
import xyz.neruxov.advertee.data.campaign.model.Campaign
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.ForbiddenException
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import java.util.*

private val lock = Any()

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class AdService(
    private val adActionRepository: AdActionRepository,
    private val campaignRepository: CampaignRepository,
    private val timeService: TimeService,
    private val clientService: ClientService,
    private val adPickerService: AdPickerService,
    private val metricsService: MetricsService
) {

    fun getRelevantAd(clientId: UUID): Ad {
        val client = clientService.getById(clientId)

        val campaign: Campaign
        synchronized(lock) {
            campaign = adPickerService.getRelevantAd(client) ?: throw NotFoundException("No relevant ads found")

            val actionId = AdAction.Id(
                campaignId = campaign.id!!,
                clientId = clientId,
                type = AdAction.Type.IMPRESSION
            )

            if (!adActionRepository.existsById(actionId)) { // учитываем только первый показ
                val action =
                    AdAction(
                        id = actionId,
                        cost = campaign.costPerImpression,
                        date = timeService.getCurrentDateInt()
                    )

                metricsService.registerNewAction(action, campaign.advertiserId)

                try {
                    adActionRepository.save(action) // иногда не успевает сохранятся :)
                } catch (ignored: DataIntegrityViolationException) {
                }
            }
        }

        return campaign.toAd()
    }

    fun registerClick(campaignId: UUID, body: AdClickRegisterRequest) {
        val campaign = campaignRepository.findById(campaignId)
            .orElseThrow { NotFoundException("Campaign with id $campaignId not found") }

        clientService.getById(body.clientId)

        val actionId = AdAction.Id(
            campaignId = campaignId,
            clientId = body.clientId,
            type = AdAction.Type.CLICK
        )

        val impressionActionId = actionId.copy(type = AdAction.Type.IMPRESSION)
        if (!adActionRepository.existsById(impressionActionId)) {
            throw ForbiddenException("You have to watch this ad before clicking on it")
        }

        if (!adActionRepository.existsById(actionId)) {
            val action =
                AdAction(
                    id = actionId,
                    cost = campaign.costPerClick,
                    date = timeService.getCurrentDateInt()
                )

            metricsService.registerNewAction(action, campaign.advertiserId)

            try {
                adActionRepository.save(action) // иногда не успевает сохранятся :)
            } catch (ignored: DataIntegrityViolationException) {
            }
        }
    }

}