package com.soundscape.controller;

import com.soundscape.dto.AuthResponse;
import com.soundscape.dto.LoginRequest;
import com.soundscape.dto.SignupRequest;
import com.soundscape.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입. 성공 시 로그인은 별도로 호출하지 않아도 되도록 userId를 바로 반환한다. */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    /** 로그인. 세션/토큰 없이 userId를 반환하며, 프론트는 이후 요청에 X-User-Id 헤더로 이 값을 사용한다. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
