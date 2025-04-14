package xyz.neruxov.advertee.service

import org.springframework.stereotype.Service
import xyz.neruxov.advertee.data.advertiser.repo.AdvertiserRepository
import xyz.neruxov.advertee.data.client.repo.ClientRepository
import xyz.neruxov.advertee.data.error.impl.NotFoundException
import xyz.neruxov.advertee.data.mlscore.model.MLScore
import xyz.neruxov.advertee.data.mlscore.repo.MLScoreRepository
import xyz.neruxov.advertee.data.mlscore.request.MLScoreRequest

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class MLScoreService(
    private val clientRepository: ClientRepository,
    private val mlScoreRepository: MLScoreRepository,
    private val advertiserRepository: AdvertiserRepository,
) {

    fun put(request: MLScoreRequest) {
        clientRepository.findById(request.clientId).orElseThrow {
            NotFoundException("Client with id ${request.clientId} not found")
        }

        advertiserRepository.findById(request.advertiserId).orElseThrow {
            NotFoundException("Advertiser with id ${request.advertiserId} not found")
        }

        mlScoreRepository.save(
            MLScore(
                id = MLScore.Id(
                    request.advertiserId, request.clientId
                ), score = request.score
            )
        )
    }

}