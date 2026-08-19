package com.afterglow.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 등록된 이메일입니다. 로그인을 이용해주세요."),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않습니다."),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 요청해주세요."),
	EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
	INVALID_EPISODE_STATE(HttpStatus.CONFLICT, "이미 진행된 단계입니다."),
	AI_REQUEST_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}
}
