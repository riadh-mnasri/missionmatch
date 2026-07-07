package com.missionmatch

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val message: String)

// A cross-cutting web concern (mapping domain exceptions to HTTP statuses) belongs at the
// composition root, not duplicated in every module's controllers: every bounded context throws
// IllegalArgumentException/IllegalStateException for invalid input or invariant violations
// (e.g. Mission.close() on an already-closed mission, Candidature.moveTo() on an illegal
// transition) and NoSuchElementException when an id doesn't resolve to anything.
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleInvalidRequest(exception: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(exception.message ?: "Invalid request"))

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(exception: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message ?: "Not found"))
}
