package com.afterglow.global.exception;

import lombok.Getter;

@Getter
public class AfterglowException extends RuntimeException {

	private final ErrorCode errorCode;

	public AfterglowException(ErrorCode errorCode) {
		super(errorCode.getDefaultMessage());
		this.errorCode = errorCode;
	}

	public AfterglowException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
