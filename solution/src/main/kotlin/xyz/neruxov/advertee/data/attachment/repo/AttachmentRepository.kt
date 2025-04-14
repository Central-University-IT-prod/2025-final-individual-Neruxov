package xyz.neruxov.advertee.data.attachment.repo

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import xyz.neruxov.advertee.data.attachment.model.Attachment
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
interface AttachmentRepository : CrudRepository<Attachment, UUID> {

    fun findAllByAdvertiserId(advertiserId: UUID, pageable: Pageable): List<Attachment>

}