package com.soundscape.controller;

import com.soundscape.dto.*;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.service.PlaylistService;
import com.soundscape.web.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlaylistController {

    private static final String USER_HEADER = "X-User-Id";

    /** 유료 OpenAI Vision 호출 남용 방지: IP 당 분당 허용 횟수. */
    private static final int PHOTO_RATE_LIMIT = 10;
    private static final Duration PHOTO_RATE_WINDOW = Duration.ofMinutes(1);

    private final PlaylistService playlistService;
    private final RateLimiter rateLimiter;

    /** 1. 사진 기반 추천 (인증 불필요, IP 기반 rate limit 적용) */
    @PostMapping(value = "/playlists/recommendations/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecommendationResponse recommendByPhoto(
            @RequestPart("image") MultipartFile image,
            HttpServletRequest request) {
        if (!rateLimiter.allow(request.getRemoteAddr(), PHOTO_RATE_LIMIT, PHOTO_RATE_WINDOW)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return playlistService.recommendByPhoto(image);
    }

    /** 2. 플리 저장 (X-User-Id 필수). request 는 JSON 파트, image 는 파일 파트로 각각 전송한다. */
    @PostMapping(value = "/playlists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistDetailResponse save(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @Valid @RequestPart("request") PlaylistSaveRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return playlistService.save(userId, request, image);
    }

    /** 3. 플리 목록 조회 (X-User-Id 필수) */
    @GetMapping("/playlists")
    public PlaylistListResponse findMyPlaylists(
            @RequestHeader(value = USER_HEADER, required = false) Long userId) {
        return new PlaylistListResponse(playlistService.findMyPlaylists(userId));
    }

    /** 4. 플리 상세 조회 (X-User-Id 필수) */
    @GetMapping("/playlists/{playlistId}")
    public PlaylistDetailResponse findDetail(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @PathVariable Long playlistId) {
        return playlistService.findDetail(userId, playlistId);
    }

    /** 5. 플리 삭제 (X-User-Id 필수) */
    @DeleteMapping("/playlists/{playlistId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @PathVariable Long playlistId) {
        playlistService.delete(userId, playlistId);
    }

}
