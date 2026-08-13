package com.afterglow.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AfterglowException.class)
	public ResponseEntity<ErrorResponse> handleAfterglowException(AfterglowException e) {
		return ResponseEntity.status(e.getErrorCode().getStatus())
				.body(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse(ErrorCode.INVALID_REQUEST.getDefaultMessage());
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream()
				.findFirst()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.orElse(ErrorCode.INVALID_REQUEST.getDefaultMessage());
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unexpected exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}
}
