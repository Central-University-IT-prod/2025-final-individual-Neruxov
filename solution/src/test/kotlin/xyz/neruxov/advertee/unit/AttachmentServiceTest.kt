package xyz.neruxov.advertee.unit

import io.awspring.cloud.s3.S3Resource
import io.awspring.cloud.s3.S3Template
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import xyz.neruxov.advertee.data.attachment.model.Attachment
import xyz.neruxov.advertee.data.attachment.repo.AttachmentRepository
import xyz.neruxov.advertee.data.campaign.repo.CampaignRepository
import xyz.neruxov.advertee.data.error.impl.*
import xyz.neruxov.advertee.service.AdvertiserService
import xyz.neruxov.advertee.service.AttachmentService
import xyz.neruxov.advertee.service.ModerationService
import xyz.neruxov.advertee.util.ImageResizer
import xyz.neruxov.advertee.util.ModelGenerators.getRandomAdvertiser
import java.io.InputStream
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 * данные тесты частично писал чатгпт
 */
class AttachmentServiceTest : StringSpec({

    val s3Template: S3Template = mockk()
    val attachmentRepository: AttachmentRepository = mockk()
    val advertiserService: AdvertiserService = mockk()
    val imageResizer: ImageResizer = mockk()
    val moderationService: ModerationService = mockk()
    val campaignRepository: CampaignRepository = mockk()

    val attachmentService = AttachmentService(
        s3Template,
        "test-bucket",
        attachmentRepository,
        advertiserService,
        imageResizer,
        moderationService,
        campaignRepository
    )

    "should throw InvalidBodyException when file is empty" {
        val file: MultipartFile = mockk {
            every { size } returns 0L
        }

        shouldThrow<InvalidBodyException> {
            attachmentService.upload(UUID.randomUUID(), file)
        }
    }

    "should throw PayloadTooLargeException when file exceeds size limit" {
        val file: MultipartFile = mockk {
            every { size } returns 512 * 1024L + 1
        }

        shouldThrow<PayloadTooLargeException> {
            attachmentService.upload(UUID.randomUUID(), file)
        }
    }

    "should throw UnsupportedMediaTypeException when file is not an image" {
        val file: MultipartFile = mockk {
            every { size } returns 1L
            every { contentType } returns "application/pdf"
            every { originalFilename } returns "test.pdf"
        }

        shouldThrow<UnsupportedMediaTypeException> {
            attachmentService.upload(UUID.randomUUID(), file)
        }
    }

    "should create and upload if everything is valid" {
        val advertiserId = UUID.randomUUID()
        val file: MultipartFile = mockk {
            every { size } returns 1L
            every { contentType } returns "image/jpeg"
            every { originalFilename } returns "test.jpg"
            every { inputStream } returns mockk()
        }

        val attachment = Attachment(UUID.randomUUID(), advertiserId, "test", "jpg", "image/jpeg")

        every { attachmentRepository.save(any<Attachment>()) } returns attachment
        every { advertiserService.getById(advertiserId) } returns getRandomAdvertiser()
        every { moderationService.isImageEnabled() } returns false
        every { s3Template.upload(any(), any(), any()) } returns mockk()

        attachmentService.upload(advertiserId, file)

        verify { attachmentRepository.save(match { it.advertiserId == advertiserId }) }
        verify { s3Template.upload("test-bucket", attachment.id.toString(), any<InputStream>()) }
    }

    "should throw NotFoundException when attachment does not exist" {
        val attachmentId = UUID.randomUUID()
        every { attachmentRepository.findById(attachmentId) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            attachmentService.getById(attachmentId)
        }
    }

    "should return an attachment when found" {
        val attachmentId = UUID.randomUUID()
        val expectedAttachment = Attachment(attachmentId, UUID.randomUUID(), "image", "jpg", "image/jpeg")
        every { attachmentRepository.findById(attachmentId) } returns Optional.of(expectedAttachment)

        val result = attachmentService.getById(attachmentId)
        result shouldBe expectedAttachment
    }

    "should get paged attachments by advertiser id" {
        val advertiserId = UUID.randomUUID()
        val attachments = (0..10).map { Attachment(UUID.randomUUID(), advertiserId, "image", "jpg", "image/jpeg") }

        every { advertiserService.getById(advertiserId) } returns getRandomAdvertiser()
        every { attachmentRepository.findAllByAdvertiserId(advertiserId, any()) } returns attachments

        val result = attachmentService.getByAdvertiserId(advertiserId, 0, 10)

        result shouldBe attachments
    }

    "should return empty list if size is 0" {
        val advertiserId = UUID.randomUUID()

        val result = attachmentService.getByAdvertiserId(advertiserId, 0, 0)

        result shouldBe emptyList()
    }

    "should return response entity with file content when downloading attachment" {
        val attachmentId = UUID.randomUUID()
        val expectedAttachment = Attachment(attachmentId, UUID.randomUUID(), "image", "jpg", "image/jpeg")
        val fileContent = "file content".toByteArray()
        val resource = ByteArrayResource(fileContent)

        every { attachmentRepository.findById(attachmentId) } returns Optional.of(expectedAttachment)
        every { s3Template.download("test-bucket", attachmentId.toString()) } returns mockk<S3Resource> {
            every { contentAsByteArray } returns resource.byteArray
        }

        val response: ResponseEntity<ByteArrayResource> = attachmentService.downloadAttachment(attachmentId, true)

        response.body!!.byteArray shouldBe fileContent
    }

    "should delete attachment" {
        val attachmentId = UUID.randomUUID()
        val attachment = Attachment(attachmentId, UUID.randomUUID(), "image", "jpg", "image/jpeg")

        every { attachmentRepository.findById(attachmentId) } returns Optional.of(attachment)
        every { s3Template.deleteObject("test-bucket", attachmentId.toString()) } just Runs
        every { attachmentRepository.delete(attachment) } just Runs
        every { campaignRepository.existsByAttachmentId(attachmentId) } returns false

        attachmentService.delete(attachmentId)

        verify { s3Template.deleteObject("test-bucket", attachmentId.toString()) }
        verify { attachmentRepository.delete(attachment) }
    }

    "should throw ConflictException if attachment is still used in some campaign(s)" {
        val attachmentId = UUID.randomUUID()
        val attachment = Attachment(attachmentId, UUID.randomUUID(), "image", "jpg", "image/jpeg")

        every { attachmentRepository.findById(attachmentId) } returns Optional.of(attachment)
        every { campaignRepository.existsByAttachmentId(attachmentId) } returns true

        shouldThrow<ConflictException> {
            attachmentService.delete(attachmentId)
        }
    }

})