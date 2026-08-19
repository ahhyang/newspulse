package com.newspulse.web;

import com.newspulse.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiError.FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toViolation)
				.toList();
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
		List<ApiError.FieldViolation> details = ex.getConstraintViolations().stream()
				.map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
				.toList();
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, details);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
	public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Malformed request", request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {}", request.getRequestURI(), ex);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
	}

	private ApiError.FieldViolation toViolation(FieldError error) {
		return new ApiError.FieldViolation(error.getField(), error.getDefaultMessage());
	}

	private ResponseEntity<ApiError> error(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			List<ApiError.FieldViolation> details
	) {
		ApiError body = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				details
		);
		return ResponseEntity.status(status).body(body);
	}
}
