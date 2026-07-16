package com.example.moneymate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

// 전체 Controller에서 나는 에러를 여기서 잡아서 처리
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation 에러 처리 (@NotBlank, @Min 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 없는 id 요청 시 에러 처리
    // ① LedgerNotFoundException 전용 — 정확히 이것만 404
    @ExceptionHandler(LedgerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleLedgerNotFound(LedgerNotFoundException e) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    // ② 그 외 예상 못한 나머지 RuntimeException — 500
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> errors = new HashMap<>();
        errors.put("message", "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }
}

// => 모든 Controller에서 에러나면 여기서 잡아서 처리