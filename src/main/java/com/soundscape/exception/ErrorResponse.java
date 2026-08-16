package com.soundscape.exception;

import java.time.LocalDateTime;

/** 공통 에러 응답 DTO: { timestamp, status, code, message } */
public record ErrorResponse(
        String timestamp,
        int status,
        String code,
        String message
) {
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message != null ? message : errorCode.getMessage()
        );
    }
}
