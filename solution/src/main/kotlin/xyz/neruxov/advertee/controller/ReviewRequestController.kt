package xyz.neruxov.advertee.controller

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.review.model.ReviewRequest
import xyz.neruxov.advertee.service.ReviewRequestService
import java.util.*

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/review-requests")
class ReviewRequestController(private val reviewRequestService: ReviewRequestService) {

    @GetMapping
    fun getReviewRequests(
        @RequestParam @Min(0, message = "Size must be greater or equal to 0") @Max(
            100, message = "Page size must be less than or equal to 100"
        ) size: Int = 20, @RequestParam @Min(0, message = "Page must be greater or equal to 0") page: Int = 0 // с нуля!
    ): List<ReviewRequest> = reviewRequestService.getReviewRequests(page = page, size = size)

    @GetMapping("/{id}")
    fun getReviewRequestById(@PathVariable id: UUID): ReviewRequest = reviewRequestService.getById(id)

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveReviewRequest(@PathVariable id: UUID): Unit =
        reviewRequestService.updateReviewRequest(id = id, verdict = true)

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rejectReviewRequest(@PathVariable id: UUID): Unit =
        reviewRequestService.updateReviewRequest(id = id, verdict = false)

}