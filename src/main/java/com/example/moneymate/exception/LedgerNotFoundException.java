package com.example.moneymate.exception;

// 존재하지 않는 거래 내역 id로 조회/수정/삭제를 시도했을 때 던지는 예외
public class LedgerNotFoundException extends RuntimeException {
    public LedgerNotFoundException(String message) {
        super(message);
    }
}