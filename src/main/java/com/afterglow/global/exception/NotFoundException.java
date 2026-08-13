package com.afterglow.global.exception;

public class NotFoundException extends AfterglowException {

	public NotFoundException(String message) {
		super(ErrorCode.NOT_FOUND, message);
	}
}
