package com.soundscape.controller;

import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.service.MusicCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 음원 추가 수급 후 서버 재시작 없이 인메모리 캐시를 갱신하기 위한 운영용 엔드포인트. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    private final MusicCacheService musicCacheService;

    /** ADMIN_API_KEY 미설정 시 항상 거부 (fail-closed). */
    @Value("${admin.api-key}")
    private String adminApiKey;

    @PostMapping("/music/reload")
    public Map<String, Object> reloadMusicCache(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String key) {
        if (adminApiKey.isBlank() || !adminApiKey.equals(key)) {
            throw new BusinessException(ErrorCode.ADMIN_UNAUTHORIZED);
        }
        int size = musicCacheService.reload();
        return Map.of("reloaded", true, "musicCount", size);
    }
}
