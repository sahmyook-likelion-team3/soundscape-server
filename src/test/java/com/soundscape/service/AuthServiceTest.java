package com.soundscape.service;

import com.soundscape.domain.User;
import com.soundscape.dto.AuthResponse;
import com.soundscape.dto.LoginRequest;
import com.soundscape.dto.SignupRequest;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void 정상_회원가입() {
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser01");
        request.setPassword("testpass123");
        request.setNickname("테스트유저");

        when(userRepository.existsByUsername("testuser01")).thenReturn(false);
        when(passwordEncoder.encode("testpass123")).thenReturn("hashed");
        User saved = User.create("testuser01", "hashed", "테스트유저");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse response = authService.signup(request);

        assertThat(response.username()).isEqualTo("testuser01");
        assertThat(response.nickname()).isEqualTo("테스트유저");
    }

    @Test
    void username_중복이면_예외() {
        SignupRequest request = new SignupRequest();
        request.setUsername("dup");
        request.setPassword("testpass123");
        request.setNickname("닉네임");

        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME));
    }

    @Test
    void 정상_로그인() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser01");
        request.setPassword("testpass123");

        User user = User.create("testuser01", "hashed", "테스트유저");
        when(userRepository.findByUsername("testuser01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("testpass123", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertThat(response.username()).isEqualTo("testuser01");
    }

    @Test
    void 존재하지_않는_username_로그인시_예외() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nouser");
        request.setPassword("testpass123");

        when(userRepository.findByUsername("nouser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void 비밀번호_불일치시_예외() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser01");
        request.setPassword("wrongpass");

        User user = User.create("testuser01", "hashed", "테스트유저");
        when(userRepository.findByUsername("testuser01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
