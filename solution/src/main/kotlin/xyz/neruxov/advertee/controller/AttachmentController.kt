package xyz.neruxov.advertee.controller

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import xyz.neruxov.advertee.data.attachment.model.Attachment
import xyz.neruxov.advertee.service.AttachmentService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/attachments")
class AttachmentController(
    private val attachmentService: AttachmentService
) {

    @GetMapping("/advertisers/{advertiserId}")
    fun getAllAttachments(
        @PathVariable advertiserId: UUID, @RequestParam @Min(0, message = "Size must be greater or equal to 0") @Max(
            100, message = "Page size must be less than or equal to 100"
        ) size: Int = 20, @RequestParam @Min(0, message = "Page must be greater or equal to 0") page: Int = 0 // с нуля!
    ): List<Attachment> = attachmentService.getByAdvertiserId(advertiserId = advertiserId, page = page, size = size)

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/advertisers/{advertiserId}", consumes = [MULTIPART_FORM_DATA_VALUE])
    fun uploadAttachment(
        @PathVariable advertiserId: UUID,
        @RequestParam file: MultipartFile
    ): Attachment = attachmentService.upload(advertiserId = advertiserId, file = file)

    @GetMapping("/{id}")
    fun getAttachment(@PathVariable id: UUID): Attachment = attachmentService.getById(id)

    @GetMapping("/{id}/content")
    fun getContent(@PathVariable id: UUID, @RequestParam download: Boolean = false): ResponseEntity<ByteArrayResource> =
        attachmentService.downloadAttachment(id, download)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAttachment(@PathVariable id: UUID): Unit = attachmentService.delete(id)

}