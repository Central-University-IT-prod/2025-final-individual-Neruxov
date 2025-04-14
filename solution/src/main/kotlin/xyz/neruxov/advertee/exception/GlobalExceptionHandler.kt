package xyz.neruxov.advertee.exception

import jakarta.servlet.ServletException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.server.MethodNotAllowedException
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import xyz.neruxov.advertee.data.error.StatusErrorException
import xyz.neruxov.advertee.data.error.impl.*

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(StatusErrorException::class)
    fun handleStatusErrorException(e: StatusErrorException): ResponseEntity<*> =
        ResponseEntity.status(e.statusCode).body(e.asMap())

    @ExceptionHandler(HttpMessageNotReadableException::class, ValidationException::class)
    fun handleHttpMessageNotReadableException(e: Exception): ResponseEntity<*> =
        handleStatusErrorException(InvalidBodyException("Invalid body"))

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<*> =
        handleStatusErrorException(PayloadTooLargeException("Request is too large"))

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<*> =
        handleStatusErrorException(InvalidBodyException("Missing ${e.parameterName} parameter of type ${e.parameterType}"))

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPartException(e: MissingServletRequestPartException): ResponseEntity<*> =
        handleStatusErrorException(InvalidBodyException("Missing ${e.requestPartName} part"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<*> =
        handleStatusErrorException(NotFoundException("Invalid ID/IDs passed"))

    @ExceptionHandler(NoSuchKeyException::class)
    fun handleNoSuchKeyException(e: NoSuchKeyException): ResponseEntity<*> =
        handleStatusErrorException(NotFoundException("Attachment not found"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<*> =
        handleStatusErrorException(InvalidBodyArgumentsException(e.fieldErrors))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<*> =
        handleStatusErrorException(
            StatusErrorException(
                400,
                "${e.name} must be of type ${e.requiredType}"
            )
        )

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<*> =
        handleStatusErrorException(
            InvalidBodyArgumentsException(
                listOf(FieldError("", "Invalid", "body"))
            )
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<*> =
        handleStatusErrorException(BodyFailedConstraintsException(e.constraintViolations))

    @ExceptionHandler(UnsupportedMediaTypeStatusException::class, HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaTypeStatusException(e: Exception): ResponseEntity<*> =
        handleStatusErrorException(UnsupportedMediaTypeException())

    @ExceptionHandler(MethodNotAllowedException::class, HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowedException(e: Exception): ResponseEntity<*> =
        handleStatusErrorException(HttpMethodNotAllowedException)

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(e: ServletException): ResponseEntity<*> =
        handleStatusErrorException(EndpointNotFoundException)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<*> {
        e.printStackTrace()
        return handleStatusErrorException(InternalServerErrorException)
    }

}