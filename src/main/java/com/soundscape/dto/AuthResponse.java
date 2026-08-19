package com.soundscape.dto;

import com.soundscape.domain.User;

/** 회원가입/로그인 공통 응답. 세션/토큰 없이 userId만 반환하며, 이후 요청은 X-User-Id 헤더로 이 값을 사용한다. */
public record AuthResponse(
        Long userId,
        String username,
        String nickname
) {
    public static AuthResponse of(User user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
