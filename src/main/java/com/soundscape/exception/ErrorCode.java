package com.soundscape.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_ID_REQUIRED(HttpStatus.UNAUTHORIZED, "USER_ID_REQUIRED", "X-User-Id 헤더가 필요합니다."),
    INVALID_USER(HttpStatus.UNAUTHORIZED, "INVALID_USER", "존재하지 않는 사용자입니다."),
    ADMIN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ADMIN_UNAUTHORIZED", "관리자 인증에 실패했습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_FORMAT", "비밀번호는 8자 이상이어야 합니다."),

    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "이미지 파일을 읽을 수 없습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "파일이 비어 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "파일 용량은 20MB를 초과할 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    INVALID_MUSIC_COUNT(HttpStatus.BAD_REQUEST, "INVALID_MUSIC_COUNT", "음악은 정확히 5곡이어야 합니다."),
    DUPLICATE_MUSIC(HttpStatus.BAD_REQUEST, "DUPLICATE_MUSIC", "중복된 음악 ID가 포함되어 있습니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED", "사진 기반 플레이리스트는 사진이 필요합니다."),

    MUSIC_NOT_FOUND(HttpStatus.NOT_FOUND, "MUSIC_NOT_FOUND", "존재하지 않는 음악이 포함되어 있습니다."),
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND", "플레이리스트를 찾을 수 없습니다."),

    VISION_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "VISION_ANALYSIS_FAILED", "사진 분석에 실패했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "S3_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
