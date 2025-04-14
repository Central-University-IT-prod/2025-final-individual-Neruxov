package xyz.neruxov.advertee.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import xyz.neruxov.advertee.data.options.request.ModerationOptions
import xyz.neruxov.advertee.service.ModerationService

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RestController
@RequestMapping("/options")
class OptionsController(
    private val moderationService: ModerationService
) {

    @GetMapping("/moderation")
    fun setModeration(): ModerationOptions = ModerationOptions(
        textEnabled = moderationService.isTextEnabled(),
        imageEnabled = moderationService.isImageEnabled()
    )

    @PostMapping("/moderation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setModeration(@RequestBody body: ModerationOptions) = moderationService.setModeration(body)

}