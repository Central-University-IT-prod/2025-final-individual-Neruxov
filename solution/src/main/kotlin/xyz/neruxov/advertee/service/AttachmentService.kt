package xyz.neruxov.advertee.service

import io.awspring.cloud.s3.S3Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import org.springframework.web.multipart.MultipartFile
import xyz.neruxov.advertee.data.attachment.model.Attachment
import xyz.neruxov.advertee.data.attachment.repo.AttachmentRepository
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.*
import xyz.neruxov.advertee.util.ImageResizer
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@Service
class AttachmentService(
    private val s3Template: S3Template,
    @Value("\${spring.cloud.aws.s3.bucket}") private val bucket: String,
    private val attachmentRepository: AttachmentRepository,
    private val advertiserService: AdvertiserService,
    private val imageResizer: ImageResizer,
    private val moderationService: ModerationService,
    private val campaignRepository: CampaignRepository
) {

    fun upload(advertiserId: UUID, file: MultipartFile): Attachment {
        if (file.size == 0L) {
            throw InvalidBodyException("File must not be empty")
        }

        if (file.size >= 512 * 1024) {
            throw PayloadTooLargeException("File size must be less than 512 KB")
        }

        val noTypeFilename = file.originalFilename?.substringBeforeLast(".") ?: "unknown"
        val extension = file.originalFilename?.substringAfterLast(".")

        val type = file.contentType ?: "application/octet-stream"
        val mimeType = MimeType.valueOf(type)

        val mediaType: MediaType
        try {
            mediaType = MediaType.parseMediaType(
                type
            )
        } catch (e: Exception) {
            throw UnsupportedMediaTypeException("Unsupported media type")
        }

        if (!listOf(MediaType.IMAGE_PNG, MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG).contains(mediaType)
        ) {
            throw UnsupportedMediaTypeException("File must be of type PNG, JPG (JPEG) or GIF")
        }

        advertiserService.getById(advertiserId)

        if (moderationService.isImageEnabled() && !isAppropriate(file, mimeType)) {
            throw InvalidBodyException("Attachment content is inappropriate")
        }

        val attachment = attachmentRepository.save(
            Attachment(
                advertiserId = advertiserId, name = noTypeFilename, extension = extension, contentType = type
            )
        )

        s3Template.upload(
            bucket, attachment.id.toString(), file.inputStream
        )

        return attachment
    }

    fun getByAdvertiserId(advertiserId: UUID, page: Int, size: Int): List<Attachment> {
        if (size == 0) return emptyList()

        advertiserService.getById(advertiserId)

        val pageable = Pageable.ofSize(size).withPage(page)
        return attachmentRepository.findAllByAdvertiserId(advertiserId, pageable)
    }

    fun getById(id: UUID): Attachment {
        return attachmentRepository.findById(id).orElseThrow { NotFoundException("Attachment with id $id not found") }
    }

    fun downloadAttachment(id: UUID, download: Boolean): ResponseEntity<ByteArrayResource> {
        val attachment = getById(id)

        val result = s3Template.download(bucket, id.toString())
        val resource = ByteArrayResource(result.contentAsByteArray)

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.contentType)).header(
            HttpHeaders.CONTENT_DISPOSITION,
            "${if (download) "attachment" else "inline"}; filename=${attachment.name}.${attachment.extension}"
        ).body(resource)
    }

    fun delete(id: UUID) {
        val attachment = getById(id)

        if (campaignRepository.existsByAttachmentId(id)) {
            throw ConflictException("Attachment is still used in some campaign(s)")
        }

        s3Template.deleteObject(bucket, id.toString())
        attachmentRepository.delete(attachment)
    }

    private fun isAppropriate(file: MultipartFile, mimeType: MimeType): Boolean {
        val resizedImage = imageResizer.resizeTo(file.bytes, 512)

        return moderationService
            .moderateImage(resizedImage, mimeType)
            .safe
    }

    init {
        try {
            if (s3Template.bucketExists(bucket)) {
                s3Template.createBucket(bucket)
            }
        } catch (ignored: Exception) {
//            e.printStackTrace()
        }
    }

}