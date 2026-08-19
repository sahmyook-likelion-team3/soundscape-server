# 회원가입 / 로그인 / 플레이리스트 삭제 API — 실행 문서 (복붙용 최종 코드)

> 이 문서는 설계 근거나 설명 없이, **실제로 적용할 때 각 파일이 최종적으로 어떤 모습이 되어야 하는지**만
> 파일 단위 전체 내용으로 담았습니다. 실행할 때는 아래 각 코드 블록을 해당 경로의 파일에 그대로
> 복사해 넣으면 됩니다 (신규 파일은 새로 생성, 기존 파일은 전체 교체).
>
> 배경/설계 이유가 필요하면 `docs/AUTH_API_HANDOFF.md`를 함께 참고하세요. 이 문서는 그 문서의 "실행 버전"입니다.
>
> **주의**: 이 문서에 적힌 대로 실제 코드에 반영하기 전에 반드시 프로젝트 소유자에게 먼저 알리고 승인받으세요.

---

## 신규 파일

### `src/main/java/com/soundscape/config/PasswordConfig.java`

```java
package com.soundscape.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### `src/main/java/com/soundscape/dto/SignupRequest.java`

```java
package com.soundscape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "username은 필수입니다.")
    @Size(min = 2, max = 12, message = "username은 2~12자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username은 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    @Size(min = 8, max = 64, message = "password는 8~64자여야 합니다.")
    private String password;

    @NotBlank(message = "nickname은 필수입니다.")
    @Size(max = 50, message = "nickname은 50자를 넘을 수 없습니다.")
    private String nickname;
}
```

### `src/main/java/com/soundscape/dto/LoginRequest.java`

```java
package com.soundscape.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "username은 필수입니다.")
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    private String password;
}
```

### `src/main/java/com/soundscape/dto/AuthResponse.java`

```java
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
```

### `src/main/java/com/soundscape/service/AuthService.java`

```java
package com.soundscape.service;

import com.soundscape.domain.User;
import com.soundscape.dto.AuthResponse;
import com.soundscape.dto.LoginRequest;
import com.soundscape.dto.SignupRequest;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        String hashed = passwordEncoder.encode(request.getPassword());
        User user = User.create(request.getUsername(), hashed, request.getNickname());
        userRepository.save(user);
        return AuthResponse.of(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return AuthResponse.of(user);
    }
}
```

### `src/main/java/com/soundscape/controller/AuthController.java`

```java
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
```

### `src/test/java/com/soundscape/service/AuthServiceTest.java`

```java
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
```

> `assertj-core`가 classpath에 없다면(`spring-boot-starter-test` 계열에는 기본 포함되지만 이 프로젝트는 `webmvc-test`/`data-jpa-test` starter를 쓰므로 실행 전 확인 필요) JUnit 기본 `Assertions`(`assertEquals`, `assertThrows`)로 바꿔서 작성하세요.

### `src/test/java/com/soundscape/service/PlaylistServiceDeleteTest.java`

```java
package com.soundscape.service;

import com.soundscape.domain.Playlist;
import com.soundscape.domain.User;
import com.soundscape.enums.SourceType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.PlaylistRepository;
import com.soundscape.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceDeleteTest {

    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MusicCacheService musicCache;
    @Mock
    private MusicRecommendationService recommendationService;
    @Mock
    private OpenAiVisionService visionService;
    @Mock
    private PhotoThemeScoringService themeScoringService;
    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PlaylistService playlistService;

    @Test
    void 정상_삭제() {
        User user = User.create("testuser01", "hashed", "테스트유저");
        Playlist playlist = Playlist.create(user, "제목", SourceType.PHOTO_RECOMMENDATION, null, "covers/abc.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playlistRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(playlist));

        playlistService.delete(1L, 10L);

        verify(s3Service).deleteObject("covers/abc.jpg");
        verify(playlistRepository).delete(playlist);
    }

    @Test
    void 존재하지_않는_플레이리스트면_404() {
        User user = User.create("testuser01", "hashed", "테스트유저");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playlistRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.delete(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }
}
```

---

## 수정 파일 (전체 최종본으로 교체)

### `build.gradle`

```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.7'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.soundscape'
version = '0.0.1-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-restclient'
	implementation 'org.springframework.security:spring-security-crypto'
	implementation platform('software.amazon.awssdk:bom:2.25.60')
	implementation 'software.amazon.awssdk:s3'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'com.mysql:mysql-connector-j'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testCompileOnly 'org.projectlombok:lombok'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
	useJUnitPlatform()
}
```

(변경점: `implementation 'org.springframework.security:spring-security-crypto'` 한 줄 추가)

### `src/main/resources/db/schema.sql`

```sql
-- ============================================================
-- SoundScape 스키마 DDL (MySQL 8.x)
-- ============================================================
CREATE DATABASE IF NOT EXISTS soundscape
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE soundscape;

DROP TABLE IF EXISTS playlist_music;
DROP TABLE IF EXISTS music_theme;
DROP TABLE IF EXISTS playlist;
DROP TABLE IF EXISTS music;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    created_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE music (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    title             VARCHAR(200)  NOT NULL,
    artist            VARCHAR(200)  NOT NULL,
    audio_object_key  VARCHAR(500)  NOT NULL,
    genre             VARCHAR(40)   NOT NULL,
    brightness        DECIMAL(4,3)  NOT NULL,
    energy            DECIMAL(4,3)  NOT NULL,
    calmness          DECIMAL(4,3)  NOT NULL,
    warmth            DECIMAL(4,3)  NOT NULL,
    positivity        DECIMAL(4,3)  NOT NULL,
    dreaminess        DECIMAL(4,3)  NOT NULL,
    duration_seconds  INT           NOT NULL,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_genre (genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE music_theme (
    music_id  BIGINT      NOT NULL,
    theme     VARCHAR(40) NOT NULL,
    PRIMARY KEY (music_id, theme),
    CONSTRAINT fk_music_theme_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    source_type       VARCHAR(20)   NOT NULL,
    source_genre      VARCHAR(40)   NULL,
    cover_object_key  VARCHAR(500)  NULL,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_playlist_user (user_id, created_at),
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_music (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    playlist_id  BIGINT NOT NULL,
    music_id     BIGINT NOT NULL,
    `position`   INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_position (playlist_id, `position`),
    CONSTRAINT fk_pm_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_music    FOREIGN KEY (music_id)    REFERENCES music (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

(변경점: `users` 테이블에 `username`, `password_hash` 컬럼 + `uk_users_username` UNIQUE 제약 추가)

### `src/main/resources/db/seed.sql`

```sql
-- ============================================================
-- SoundScape 시드 데이터 (users 1명 + music 26곡 + music_theme 다중매핑)
-- 실행 순서: schema.sql -> seed.sql
-- ============================================================
USE soundscape;

-- [0. users 시드] X-User-Id: 1 임시 인증용 데모 계정. 없으면 플리 저장 시 FK 제약 위반 발생.
-- username: demo_user / password: demo1234 (아래 해시는 BCryptPasswordEncoder.encode("demo1234") 예시 결과 형식 -- 실제 실행 전 애플리케이션에서 직접 생성한 해시로 교체할 것)
INSERT INTO users (id, username, password_hash, nickname, created_at)
VALUES (1, 'demo_user', '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH', 'demo_user', NOW());

-- [1. music 테이블 시드 데이터 26곡]
INSERT INTO music (id, title, artist, audio_object_key, genre, brightness, energy, calmness, warmth, positivity, dreaminess, duration_seconds, created_at, updated_at) VALUES
(1, 'Ask', 'lofizoo_music', 'music/ask.mp3', 'LOFI_INSTRUMENTAL', 0.45, 0.35, 0.85, 0.70, 0.50, 0.75, 124, NOW(), NOW()),
(2, 'Smooth', 'lofizoo_music', 'music/smooth.mp3', 'LOFI_INSTRUMENTAL', 0.48, 0.31, 0.83, 0.72, 0.54, 0.79, 122, NOW(), NOW()),
(3, 'Dark', 'lofizoo_music', 'music/dark.mp3', 'LOFI_INSTRUMENTAL', 0.18, 0.24, 0.89, 0.37, 0.28, 0.92, 75, NOW(), NOW()),
(4, 'BLACK BOX - Chill 2', 'BLACKBOX', 'music/blackbox_chill2.mp3', 'LOFI_INSTRUMENTAL', 0.52, 0.28, 0.86, 0.81, 0.58, 0.74, 88, NOW(), NOW()),
(5, 'Study Music Session', 'alex-morgan', 'music/study_session.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.58, 0.34, 0.88, 0.76, 0.63, 0.42, 181, NOW(), NOW()),
(6, 'Ambient Background Soft Air Drift', 'alex-morgan', 'music/soft_air_drift.mp3', 'LOFI_INSTRUMENTAL', 0.38, 0.12, 0.96, 0.64, 0.48, 0.84, 190, NOW(), NOW()),
(7, 'Geography Study', 'The_Mountain', 'music/geography_study.mp3', 'LOFI_INSTRUMENTAL', 0.61, 0.52, 0.67, 0.43, 0.68, 0.34, 127, NOW(), NOW()),
(8, 'Calm Ambient', 'Kulakovka', 'music/calm_ambient.mp3', 'LOFI_INSTRUMENTAL', 0.64, 0.18, 0.94, 0.86, 0.71, 0.78, 135, NOW(), NOW()),
(9, 'Study Vibe', 'The_Mountain', 'music/study_vibe.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.59, 0.27, 0.91, 0.84, 0.66, 0.72, 134, NOW(), NOW()),
(10, 'Ambient Forest Rain', 'NourishedByMusic', 'music/forest_rain.mp3', 'LOFI_INSTRUMENTAL', 0.32, 0.14, 0.97, 0.58, 0.64, 0.89, 294, NOW(), NOW()),
(11, 'Sparkling Glasses and Nocturnal Tunes', 'HarumachiMusic', 'music/nocturnal_tunes.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.62, 0.38, 0.87, 0.89, 0.74, 0.68, 171, NOW(), NOW()),
(12, 'Think about yesterday', 'HarumachiMusic', 'music/think_yesterday.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.53, 0.21, 0.93, 0.92, 0.69, 0.76, 198, NOW(), NOW()),
(13, 'Relaxing Music Coffee Shop', 'alex-morgan', 'music/relaxing_coffee_shop.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.63, 0.32, 0.89, 0.84, 0.72, 0.52, 160, NOW(), NOW()),
(14, 'Relax - Relaxing Background', 'andriig', 'music/relax_background.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.56, 0.19, 0.94, 0.90, 0.73, 0.78, 139, NOW(), NOW()),
(15, 'Acoustic Guitar Sunrise Travel', 'alex-morgan', 'music/sunrise_travel.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.78, 0.44, 0.81, 0.86, 0.84, 0.61, 149, NOW(), NOW()),
(16, 'Whisper of the Breeze', 'HirohaSaimoto', 'music/whisper_breeze.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.71, 0.23, 0.92, 0.87, 0.78, 0.73, 213, NOW(), NOW()),
(17, 'Whisper of the Pines', 'HirohaSaimoto', 'music/whisper_pines.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.76, 0.37, 0.88, 0.85, 0.83, 0.66, 240, NOW(), NOW()),
(18, 'feel the breeze', 'HarumachiMusic', 'music/feel_breeze.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.74, 0.42, 0.83, 0.86, 0.81, 0.63, 153, NOW(), NOW()),
(19, 'Dusty Roads', 'FreeMusicLab', 'music/dusty_roads.mp3', 'POP', 0.82, 0.84, 0.21, 0.63, 0.86, 0.32, 112, NOW(), NOW()),
(20, 'I TRAVEL', 'AlisiaBeats', 'music/i_travel.mp3', 'POP', 0.88, 0.79, 0.26, 0.68, 0.89, 0.41, 103, NOW(), NOW()),
(21, 'American Trip', 'Sonican', 'music/american_trip.mp3', 'POP', 0.83, 0.66, 0.42, 0.84, 0.87, 0.48, 62, NOW(), NOW()),
(22, 'Traveling Colours', 'Oleg-Mazur', 'music/traveling_colours.mp3', 'POP', 0.79, 0.72, 0.35, 0.62, 0.84, 0.56, 190, NOW(), NOW()),
(23, 'BLACK BOX - Vlog 1', 'BLACKBOX', 'music/blackbox_vlog1.mp3', 'POP', 0.91, 0.64, 0.48, 0.79, 0.92, 0.38, 130, NOW(), NOW()),
(24, 'Acoustic Indie Folk', 'Markjuly', 'music/indie_folk.mp3', 'POP', 0.85, 0.68, 0.51, 0.89, 0.88, 0.44, 131, NOW(), NOW()),
(25, 'Lazy Summer Days', 'HoggyArt', 'music/summer_days.mp3', 'POP', 0.81, 0.58, 0.62, 0.82, 0.86, 0.53, 127, NOW(), NOW()),
(26, 'Inspire Indie Acoustic', 'ONALDIN_Music', 'music/indie_acoustic.mp3', 'POP', 0.87, 0.73, 0.46, 0.86, 0.91, 0.52, 158, NOW(), NOW());

-- [2. music_theme 테이블 시드 데이터 다중 테마 매핑]
INSERT INTO music_theme (music_id, theme) VALUES
(1, 'STUDY_FOCUS'), (1, 'REST_HEALING'), (1, 'NIGHT_DREAMY'),
(2, 'STUDY_FOCUS'), (2, 'REST_HEALING'), (2, 'NIGHT_DREAMY'),
(3, 'NIGHT_DREAMY'), (3, 'RAINY_DAY'), (3, 'STUDY_FOCUS'),
(4, 'REST_HEALING'), (4, 'STUDY_FOCUS'), (4, 'RAINY_DAY'),
(5, 'STUDY_FOCUS'), (5, 'REST_HEALING'),
(6, 'STUDY_FOCUS'), (6, 'REST_HEALING'), (6, 'NIGHT_DREAMY'),
(7, 'STUDY_FOCUS'), (7, 'BRIGHT_DAY'),
(8, 'REST_HEALING'), (8, 'STUDY_FOCUS'), (8, 'BRIGHT_DAY'),
(9, 'STUDY_FOCUS'), (9, 'REST_HEALING'), (9, 'NIGHT_DREAMY'),
(10, 'RAINY_DAY'), (10, 'STUDY_FOCUS'), (10, 'REST_HEALING'),
(11, 'REST_HEALING'), (11, 'NIGHT_DREAMY'), (11, 'STUDY_FOCUS'),
(12, 'REST_HEALING'), (12, 'WALK_DRIVE_TRAVEL'), (12, 'NIGHT_DREAMY'),
(13, 'REST_HEALING'), (13, 'STUDY_FOCUS'), (13, 'BRIGHT_DAY'),
(14, 'REST_HEALING'), (14, 'WALK_DRIVE_TRAVEL'), (14, 'NIGHT_DREAMY'),
(15, 'REST_HEALING'), (15, 'WALK_DRIVE_TRAVEL'), (15, 'BRIGHT_DAY'),
(16, 'REST_HEALING'), (16, 'WALK_DRIVE_TRAVEL'), (16, 'BRIGHT_DAY'),
(17, 'REST_HEALING'), (17, 'WALK_DRIVE_TRAVEL'), (17, 'BRIGHT_DAY'),
(18, 'REST_HEALING'), (18, 'WALK_DRIVE_TRAVEL'), (18, 'BRIGHT_DAY'),
(19, 'WALK_DRIVE_TRAVEL'), (19, 'PARTY_ENERGY'), (19, 'BRIGHT_DAY'),
(20, 'WALK_DRIVE_TRAVEL'), (20, 'BRIGHT_DAY'), (20, 'PARTY_ENERGY'),
(21, 'WALK_DRIVE_TRAVEL'), (21, 'BRIGHT_DAY'), (21, 'REST_HEALING'),
(22, 'WALK_DRIVE_TRAVEL'), (22, 'BRIGHT_DAY'),
(23, 'WALK_DRIVE_TRAVEL'), (23, 'BRIGHT_DAY'), (23, 'REST_HEALING'),
(24, 'WALK_DRIVE_TRAVEL'), (24, 'BRIGHT_DAY'), (24, 'REST_HEALING'),
(25, 'WALK_DRIVE_TRAVEL'), (25, 'BRIGHT_DAY'), (25, 'REST_HEALING'),
(26, 'WALK_DRIVE_TRAVEL'), (26, 'BRIGHT_DAY'), (26, 'REST_HEALING');
```

(변경점: `INSERT INTO users` 문에 `username`, `password_hash` 컬럼/값 추가. `<BCrypt 해시>` 부분은 실제 실행 전 진짜 해시값으로 교체 필요)

### `src/main/java/com/soundscape/domain/User.java`

```java
package com.soundscape.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private User(String username, String passwordHash, String nickname) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    public static User create(String username, String passwordHash, String nickname) {
        return new User(username, passwordHash, nickname);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

### `src/main/java/com/soundscape/exception/ErrorCode.java`

```java
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
```

(변경점: `DUPLICATE_USERNAME`, `INVALID_CREDENTIALS`, `INVALID_PASSWORD_FORMAT` 3개 추가. 그 외 기존 항목은 순서/내용 변경 없음)

### `src/main/java/com/soundscape/repository/UserRepository.java`

```java
package com.soundscape.repository;

import com.soundscape.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
}
```

### `src/main/java/com/soundscape/repository/PlaylistRepository.java`

```java
package com.soundscape.repository;

import com.soundscape.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    @Query("select distinct p from Playlist p left join fetch p.items where p.user.id = :userId order by p.createdAt desc")
    List<Playlist> findAllByUserIdWithItems(@Param("userId") Long userId);

    @Query("select distinct p from Playlist p left join fetch p.items where p.id = :playlistId and p.user.id = :userId")
    Optional<Playlist> findByIdAndUserIdWithItems(@Param("playlistId") Long playlistId, @Param("userId") Long userId);

    @Query("select p from Playlist p where p.id = :playlistId and p.user.id = :userId")
    Optional<Playlist> findByIdAndUserId(@Param("playlistId") Long playlistId, @Param("userId") Long userId);
}
```

(변경점: `findByIdAndUserId` 메서드 추가, 나머지 동일)

### `src/main/java/com/soundscape/service/S3Service.java`

```java
package com.soundscape.service;

import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presign-minutes}")
    private long presignMinutes;

    /** 플리 커버(원본 사진) 업로드 후 object key 반환. */
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String key = "covers/" + UUID.randomUUID() + resolveExtension(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
            return key;
        } catch (IOException | RuntimeException e) {
            log.error("[S3] 업로드 실패 key={}", key, e);
            throw new BusinessException(ErrorCode.S3_UPLOAD_FAILED, e);
        }
    }

    /** object key -> Presigned GET URL. key 가 null 이면 null 반환. */
    public String presignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /** object key로 S3 객체 삭제. key가 null이면 아무것도 하지 않는다. 실패해도 예외를 던지지 않고 로그만 남긴다(삭제 흐름 자체를 막지 않기 위함). */
    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (RuntimeException e) {
            log.warn("[S3] 삭제 실패 key={}", objectKey, e);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot).toLowerCase() : "";
    }
}
```

(변경점: `import ...DeleteObjectRequest` 추가, `deleteObject()` 메서드 추가. 나머지 동일)

### `src/main/java/com/soundscape/service/PlaylistService.java`

```java
package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.domain.Playlist;
import com.soundscape.domain.PlaylistMusic;
import com.soundscape.domain.User;
import com.soundscape.dto.*;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.enums.SourceType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.PlaylistRepository;
import com.soundscape.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final MusicCacheService musicCache;
    private final MusicRecommendationService recommendationService;
    private final OpenAiVisionService visionService;
    private final PhotoThemeScoringService themeScoringService;
    private final S3Service s3Service;

    /** 사진 기반 추천. 저장은 하지 않고 5곡 Presigned URL 만 반환한다. */
    public RecommendationResponse recommendByPhoto(MultipartFile photo) {
        PhotoAnalysisResult analysis = visionService.analyze(photo);
        List<PlaylistTheme> themes = themeScoringService.score(analysis);
        List<MusicResponse> musics = toMusicResponses(recommendationService.recommendByPhoto(analysis, themes));
        return RecommendationResponse.photo(analysis, themes, musics);
    }

    /** 플리 저장: 원본 사진 S3 업로드 + 플리 + 곡 position 저장. */
    @Transactional
    public PlaylistDetailResponse save(Long userId, PlaylistSaveRequest request, MultipartFile photo) {
        User user = requireUser(userId);
        validateSaveRequest(request, photo);

        String coverObjectKey = s3Service.uploadCover(photo);

        Playlist playlist = Playlist.create(
                user,
                request.getTitle(),
                request.getSourceType(),
                null,
                coverObjectKey
        );
        playlist.replaceItems(request.getMusicIds());
        playlistRepository.save(playlist);

        return toDetail(playlist);
    }

    /** 내 보관함 목록. 커버 Presigned URL 포함. */
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> findMyPlaylists(Long userId) {
        requireUser(userId);
        return playlistRepository.findAllByUserIdWithItems(userId).stream()
                .map(p -> PlaylistSummaryResponse.of(p, s3Service.presignedUrl(p.getCoverObjectKey())))
                .toList();
    }

    /** 플리 상세. 조회 시점에 오디오 Presigned URL 을 재발급한다. */
    @Transactional(readOnly = true)
    public PlaylistDetailResponse findDetail(Long userId, Long playlistId) {
        requireUser(userId);
        Playlist playlist = playlistRepository.findByIdAndUserIdWithItems(playlistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
        return toDetail(playlist);
    }

    /** 플리 삭제. 본인 소유가 아니거나 존재하지 않으면 404 (소유권 정보를 노출하지 않기 위해 403이 아닌 404로 통일). */
    @Transactional
    public void delete(Long userId, Long playlistId) {
        requireUser(userId);
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

        s3Service.deleteObject(playlist.getCoverObjectKey());
        playlistRepository.delete(playlist);
    }

    /** 저장 요청 검증: 곡 수, 중복, 존재 여부, 사진 필수 여부. 현재 MVP는 사진 기반만 지원한다. */
    private void validateSaveRequest(PlaylistSaveRequest request, MultipartFile photo) {
        List<Long> musicIds = request.getMusicIds();

        if (musicIds.size() != MusicRecommendationService.RESULT_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_MUSIC_COUNT);
        }
        if (musicIds.stream().distinct().count() != musicIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_MUSIC);
        }
        if (!musicCache.existsAll(musicIds)) {
            throw new BusinessException(ErrorCode.MUSIC_NOT_FOUND);
        }
        if (request.getSourceType() != SourceType.PHOTO_RECOMMENDATION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_REQUIRED);
        }
    }

    private PlaylistDetailResponse toDetail(Playlist playlist) {
        List<MusicResponse> musics = playlist.getItems().stream()
                .sorted(Comparator.comparingInt(PlaylistMusic::getPosition))
                .map(item -> {
                    Music music = musicCache.findById(item.getMusicId());
                    return music == null ? null
                            : MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), null, item.getPosition());
                })
                .filter(Objects::nonNull)
                .toList();

        return PlaylistDetailResponse.of(playlist, s3Service.presignedUrl(playlist.getCoverObjectKey()), musics);
    }

    private List<MusicResponse> toMusicResponses(List<MusicRecommendationService.Scored> scoredList) {
        List<MusicResponse> result = new java.util.ArrayList<>(scoredList.size());
        for (int i = 0; i < scoredList.size(); i++) {
            MusicRecommendationService.Scored scored = scoredList.get(i);
            Music music = scored.music();
            Double score = scored.score() == null
                    ? null
                    : Math.round(scored.score() * 10000) / 10000.0;
            result.add(MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), score, i + 1));
        }
        return result;
    }

    /** 임시 인증: X-User-Id 헤더 검증. 미전송 401, 미존재 사용자 401. */
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USER));
    }
}
```

(변경점: `delete()` 메서드 추가(`findDetail` 바로 아래). 나머지 동일)

### `src/main/java/com/soundscape/config/WebConfig.java`

```java
package com.soundscape.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 허용 origin 은 app.cors.allowed-origins (CORS_ALLOWED_ORIGINS 환경변수) 로 설정한다. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-User-Id")
                .exposedHeaders("Location")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
```

(변경점: `allowedMethods`에 `"DELETE"` 추가. **이걸 빠뜨리면 삭제 API를 배포해도 브라우저 CORS 프리플라이트 단계에서 막혀 프론트의 `deletePlaylist()` 호출이 전부 실패합니다** — 실제로 배포된 HTTPS 엔드포인트에 curl로 OPTIONS 프리플라이트를 날려 `Access-Control-Allow-Methods`에 DELETE가 없는 것을 확인해서 찾은 문제였습니다.)

### `src/main/java/com/soundscape/controller/PlaylistController.java`

```java
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
```

(변경점: `delete()` 엔드포인트 추가(`findDetail` 바로 아래). 나머지 동일)

---

## 운영 DB 마이그레이션 SQL (코드 아님 — 별도 실행 대상, 반드시 승인 후 실행)

```sql
ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NULL AFTER id,
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER username;

UPDATE users SET username = 'demo_user', password_hash = '<실제 BCrypt 해시로 교체>' WHERE id = 1;

ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    MODIFY COLUMN password_hash VARCHAR(100) NOT NULL,
    ADD UNIQUE KEY uk_users_username (username);
```

---

## 적용 순서 체크리스트

1. `build.gradle` 교체 → `./gradlew build`로 의존성 다운로드 확인
2. 신규 파일 6개(`PasswordConfig`, `SignupRequest`, `LoginRequest`, `AuthResponse`, `AuthService`, `AuthController`) 생성
3. 기존 파일 8개(`User`, `ErrorCode`, `UserRepository`, `PlaylistRepository`, `S3Service`, `PlaylistService`, `PlaylistController`, `WebConfig`) 교체 — **`WebConfig`의 `allowedMethods`에 `DELETE` 추가 빠뜨리지 말 것** (안 하면 삭제 API가 CORS로 막힘)
4. `schema.sql`, `seed.sql` 교체 (로컬 DB 재생성 시에만 적용됨. 운영 DB는 위 마이그레이션 SQL로 별도 처리)
5. 테스트 파일 2개(`AuthServiceTest`, `PlaylistServiceDeleteTest`) 생성 → `./gradlew test`
6. 로컬 DB 재생성 후 curl로 signup/login/delete 동작 확인
