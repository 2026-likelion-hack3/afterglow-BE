package com.afterglow.domain.account.domain;

/** 계정이 삭제됐다는 사실만 알린다 — 어떤 도메인이 어떻게 정리하는지는 Account가 알지 못한다. */
public record AccountDeletedEvent(Long accountId) {
}
