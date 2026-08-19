# 회원가입/로그인/플레이리스트 삭제 API 구현 인수인계 문서

> 이 문서는 작업 도중 토큰이 소진되어 세션이 끊기더라도, 다른 AI(예: 안티그래비티+Gemini)가
> 이 문서만 보고 이어서 작업을 완료할 수 있도록 작성되었습니다. 모든 파일 경로, 코드 스니펫,
> 기존 컨벤션을 그대로 명시했으니 **추측하지 말고 아래 내용을 그대로 따라 구현하세요.**

## 0. 작업 전 필수 확인사항

- 이 저장소는 SoundScape 백엔드(Spring Boot 4.0.7, Java 17, MySQL 8)입니다. 저장소 루트: `C:\Users\김상진\soundscape\soundscape`
- **백엔드 코드를 수정하기 전에 반드시 사용자(프로젝트 소유자)에게 먼저 변경 내용을 설명하고 승인을 받으세요.** 이 프로젝트의 확립된 규칙입니다. 승인 없이 파일을 만들거나 수정하지 마세요.
- 프론트엔드 저장소(`soundscape_front`)는 이 작업 범위에 포함되지 않습니다. 백엔드 API만 구현합니다. (단, 이 문서 8절에 프론트 연동 가이드는 참고용으로 남겨둡니다.)
- 목표 1: 실제 아이디+비밀번호 기반 회원가입/로그인 API 추가. 로그인 성공 시 세션이나 JWT를 발급하지 않고 **user id만 반환**하며, 이후 요청은 기존 방식 그대로 `X-User-Id` 헤더로 인증합니다(이 프로젝트의 기존 인증 구조를 그대로 유지하는 것이 핵심 설계 결정입니다. 세션/토큰 인프라를 새로 만들지 마세요).
- 목표 2: 플레이리스트 삭제 API 추가. 저장(`POST /playlists`), 목록(`GET /playlists`), 상세(`GET /playlists/{id}`)는 이미 있는데 삭제만 빠져 있던 기능입니다(직전 커밋 `9e5224c`에서 통합 테스트로 이미 이슈로 기록되어 있던 부재 API 중 하나). 9절, 9-A절에 상세 스펙이 있습니다.

## 1. 현재 코드 상태 (구현 전 baseline)

### 1-1. `User` 엔티티 — `src/main/java/com/soundscape/domain/User.java`
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

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```
지금은 `nickname`과 `createdAt`만 있고, 로그인용 아이디/비밀번호 컬럼이 전혀 없습니다. 정적 팩토리 메서드(`create`)도 없습니다.

### 1-2. `users` 테이블 스키마 — `src/main/resources/db/schema.sql`
```sql
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1-3. 시드 데이터 — `src/main/resources/db/seed.sql`
```sql
-- [0. users 시드] X-User-Id: 1 임시 인증용 데모 계정. 없으면 플리 저장 시 FK 제약 위반 발생.
INSERT INTO users (id, nickname, created_at) VALUES (1, 'demo_user', NOW());
```
이 `id=1` 데모 유저는 배포된 DB에 이미 존재하고, 플레이리스트 등 다른 테이블이 FK로 참조하고 있을 수 있습니다. **삭제하지 말고, 로그인 가능한 계정으로 마이그레이션**하세요 (5절 참고).

### 1-4. 에러 코드 — `src/main/java/com/soundscape/exception/ErrorCode.java`
`HttpStatus`, `code` 문자열, 한글 메시지 3-tuple로 구성된 enum입니다. 새 에러코드는 이 enum에 항목을 추가하는 방식으로만 만듭니다 (별도 예외 클래스 만들지 마세요).

### 1-5. 예외 처리 — `GlobalExceptionHandler` / `BusinessException`
- `BusinessException(ErrorCode)`를 던지면 `GlobalExceptionHandler`가 자동으로 `{ timestamp, status, code, message }` 형태의 `ErrorResponse`로 변환합니다. **새 예외 핸들러를 추가할 필요 없습니다.**
- `@Valid` DTO 검증 실패는 `BindException`으로 자동 처리되어 `INVALID_REQUEST`로 응답됩니다.

### 1-6. 인증 방식 — `PlaylistService.requireUser()`
```java
/** 임시 인증: X-User-Id 헤더 검증. 미전송 401, 미존재 사용자 401. */
private User requireUser(Long userId) {
    if (userId == null) {
        throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
    }
    return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USER));
}
```
로그인 API를 추가해도 이 패턴(`X-User-Id` 헤더 신뢰)은 **그대로 유지**합니다. Spring Security, 세션, JWT는 도입하지 않습니다.

### 1-7. 빌드 의존성 — `build.gradle`
`spring-security-crypto` 계열 의존성이 전혀 없습니다. 비밀번호 해싱(BCrypt)을 위해 의존성 추가가 필요합니다 (2-6절 참고). **`spring-boot-starter-security`는 추가하지 마세요** — 이걸 추가하면 Spring Security의 기본 인증 필터가 자동 활성화되어 기존의 모든 공개 API(`/api/v1/playlists/recommendations/photo` 등)가 막혀버립니다. 반드시 `spring-security-crypto` 단독 의존성만 추가합니다.

### 1-8. 컨트롤러 스타일 — `src/main/java/com/soundscape/controller/PlaylistController.java`
```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlaylistController {
    private static final String USER_HEADER = "X-User-Id";
    ...
    @PostMapping(value = "/playlists/recommendations/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecommendationResponse recommendByPhoto(...) { ... }
}
```
새 `AuthController`도 `/api/v1` 하위, JSON body(`application/json`) 방식, `@RequiredArgsConstructor` + 서비스 위임 패턴을 그대로 따릅니다.

### 1-9. DTO 검증 스타일 — `src/main/java/com/soundscape/dto/PlaylistSaveRequest.java`
`@Getter @Setter` + `jakarta.validation.constraints.*` 애노테이션을 필드에 직접 붙이는 방식을 사용합니다. Record가 아니라 일반 클래스입니다 (Lombok Setter가 필요해서 — 멀티파트 바인딩용). 새 인증 DTO는 request 는 이 스타일, response 는 `ErrorResponse`처럼 **record**로 작성합니다 (기존 response DTO들도 record 스타일 확인 필요 — `MusicResponse`, `PlaylistDetailResponse` 등 참고).

---

## 2. 구현할 파일 목록 (신규/수정)

| # | 파일 경로 | 작업 |
|---|---|---|
| 1 | `src/main/java/com/soundscape/domain/User.java` | 수정: `username`, `passwordHash` 필드 + 정적 팩토리 추가 |
| 2 | `src/main/resources/db/schema.sql` | 수정: `users` 테이블에 컬럼 추가 |
| 3 | `src/main/resources/db/seed.sql` | 수정: demo_user에 username/password 부여 |
| 4 | `src/main/java/com/soundscape/exception/ErrorCode.java` | 수정: 에러코드 3개 추가 |
| 5 | `src/main/java/com/soundscape/repository/UserRepository.java` | 수정: `existsByUsername`, `findByUsername` 추가 |
| 6 | `src/main/java/com/soundscape/dto/SignupRequest.java` | 신규 |
| 7 | `src/main/java/com/soundscape/dto/LoginRequest.java` | 신규 |
| 8 | `src/main/java/com/soundscape/dto/AuthResponse.java` | 신규 |
| 9 | `src/main/java/com/soundscape/service/AuthService.java` | 신규 |
| 10 | `src/main/java/com/soundscape/controller/AuthController.java` | 신규 |
| 11 | `src/main/java/com/soundscape/config/PasswordConfig.java` | 신규: `BCryptPasswordEncoder` 빈 등록 |
| 12 | `build.gradle` | 수정: `spring-security-crypto` 의존성 추가 |
| 13 | `src/test/java/com/soundscape/service/AuthServiceTest.java` | 신규 |
| 14 | `README.md` | 수정: API 스펙 섹션에 회원가입/로그인/삭제 추가 |
| 15 | `src/main/java/com/soundscape/repository/PlaylistRepository.java` | 수정: 삭제용 소유권 조회 쿼리 추가 |
| 16 | `src/main/java/com/soundscape/service/S3Service.java` | 수정: `deleteObject()` 추가 |
| 17 | `src/main/java/com/soundscape/service/PlaylistService.java` | 수정: `delete()` 메서드 추가 |
| 18 | `src/main/java/com/soundscape/controller/PlaylistController.java` | 수정: `DELETE /playlists/{playlistId}` 추가 |
| 19 | `src/test/java/com/soundscape/service/PlaylistServiceDeleteTest.java` | 신규 |

---

## 3. 상세 구현 스펙

### 3-1. `build.gradle` — 의존성 추가

`dependencies { ... }` 블록에 아래 한 줄만 추가합니다 (Spring Security 전체가 아니라 crypto 모듈만):

```groovy
implementation 'org.springframework.security:spring-security-crypto'
```

### 3-2. `PasswordConfig.java` (신규) — BCrypt 빈 등록

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

### 3-3. `schema.sql` 수정 — `users` 테이블

기존:
```sql
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

변경 후:
```sql
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    created_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- `username`: 로그인 아이디. 영문/숫자 조합 권장, 중복 불가 → `UNIQUE` 제약으로 DB 레벨에서도 보장.
- `password_hash`: BCrypt 해시 결과는 항상 60자 고정이지만 여유 있게 100자로 설정 (기존 프로젝트의 다른 VARCHAR 컬럼들도 여유값 사용하는 컨벤션).
- `nickname`: 기존 컬럼 그대로 유지 (화면 표시용 별명, username과 별개 — 회원가입 시 같이 입력받음).

이 프로젝트는 `schema.sql`이 `DROP TABLE IF EXISTS` 후 `CREATE TABLE`을 다시 하는 방식(전체 재생성)이라는 점에 유의하세요. 즉 로컬/데모 환경에서는 이 파일을 실행하면 테이블이 통째로 재생성됩니다. **운영 DB(가비아 서버의 MySQL)에는 이미 데이터가 있으므로, `schema.sql`을 그대로 재실행하면 안 되고 `ALTER TABLE`로 반영해야 합니다** (5절 마이그레이션 절차 참고).

### 3-4. `User.java` 수정 — 엔티티

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
`Playlist.create(...)` 정적 팩토리 패턴(비공개 생성자 + `public static create`)을 그대로 따랐습니다.

### 3-5. `ErrorCode.java` 수정 — 에러코드 3개 추가

`USER_ID_REQUIRED` 항목 근처(인증 관련 그룹)에 아래 3개를 추가합니다:

```java
DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다."),
INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_FORMAT", "비밀번호는 8자 이상이어야 합니다."),
```

주의: `INVALID_CREDENTIALS`는 "아이디가 없는지, 비밀번호가 틀렸는지"를 구분하지 않고 동일한 메시지로 응답합니다. 이는 실수가 아니라 **의도된 보안 설계**입니다 — 아이디 존재 여부를 노출하면 계정 목록을 무작위 대입으로 스캔당할 수 있으므로, 로그인 실패 시에는 항상 이 메시지 하나로 통일합니다. (반면 회원가입 시 아이디 중복 체크는 `DUPLICATE_USERNAME`으로 명확히 알려줘야 사용자가 다른 아이디를 고를 수 있으므로 구분됩니다.)

### 3-6. `UserRepository.java` 수정

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

### 3-7. `SignupRequest.java` (신규 DTO)

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
    @Size(min = 4, max = 20, message = "username은 4~20자여야 합니다.")
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

### 3-8. `LoginRequest.java` (신규 DTO)

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

### 3-9. `AuthResponse.java` (신규 DTO, record)

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

### 3-10. `AuthService.java` (신규)

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

`PlaylistService`와 동일하게 `@Transactional`/`@Transactional(readOnly = true)`를 메서드 단위로 명시하는 컨벤션을 따랐습니다.

### 3-11. `AuthController.java` (신규)

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

`@RequestBody` + `application/json`으로 받습니다 (기존 `PlaylistController`는 멀티파트를 다루지만, 인증 API는 파일 업로드가 없으므로 순수 JSON입니다. `MediaType` 명시 없이 기본값 사용).

---

## 4. 유효성 검증 / 에러 케이스 정리

| 케이스 | 처리 방식 | 응답 |
|---|---|---|
| 회원가입 시 username 미입력/형식 오류 | `@Valid` → `BindException` | 400 `INVALID_REQUEST` |
| 회원가입 시 password 8자 미만 | `@Valid` (`@Size(min=8)`) | 400 `INVALID_REQUEST` |
| 회원가입 시 username 중복 | `AuthService.signup`에서 `existsByUsername` 체크 | 409 `DUPLICATE_USERNAME` |
| 로그인 시 존재하지 않는 username | `AuthService.login`에서 `findByUsername` 실패 | 401 `INVALID_CREDENTIALS` |
| 로그인 시 비밀번호 불일치 | `passwordEncoder.matches` 실패 | 401 `INVALID_CREDENTIALS` |
| 회원가입/로그인 request body가 JSON이 아니거나 파싱 불가 | 기존 `GlobalExceptionHandler.handleMalformedRequest`가 이미 처리 (수정 불필요) | 400 `INVALID_REQUEST` |

`ErrorCode.INVALID_PASSWORD_FORMAT`은 `@Size` 애노테이션의 메시지로 이미 커버되므로 컨트롤러/서비스에서 직접 던질 일은 없지만, 향후 "비밀번호는 영문+숫자 조합 필수" 같은 정규식 검증을 서비스 레이어에서 별도로 추가하고 싶을 경우를 위해 미리 정의해둔 것입니다. **MVP 범위에서는 사용하지 않아도 무방합니다.**

---

## 5. 기존 데모 유저(`id=1`) 마이그레이션 절차

배포된 가비아 서버의 MySQL에는 이미 `users` 테이블에 `id=1, nickname='demo_user'` 레코드가 있고, 다른 테이블(playlist 등)이 이를 FK로 참조하고 있을 가능성이 높습니다. 컬럼을 추가하면서 이 레코드를 깨뜨리면 안 됩니다.

**운영 DB에 적용할 마이그레이션 SQL** (schema.sql 재실행이 아니라 이 SQL을 직접 실행):

```sql
ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NULL AFTER id,
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER username;

-- 기존 demo_user에게 임시 로그인 정보 부여 (비밀번호는 반드시 BCrypt 해시로 넣어야 함 — 아래는 예시일 뿐 평문 저장 금지)
-- 실제 해시값은 BCryptPasswordEncoder로 애플리케이션 코드에서 생성한 값을 사용할 것.
UPDATE users SET username = 'demo_user', password_hash = '<BCrypt 해시값>' WHERE id = 1;

ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    MODIFY COLUMN password_hash VARCHAR(100) NOT NULL,
    ADD UNIQUE KEY uk_users_username (username);
```

절차: 컬럼을 일단 NULL 허용으로 추가 → 기존 행 값 채움 → 그 다음에 NOT NULL + UNIQUE 제약을 겁니다 (컬럼 추가와 동시에 NOT NULL을 걸면 기존 행 때문에 실패합니다).

BCrypt 해시값 생성 방법: 로컬에서 임시 테스트 코드나 `BCryptPasswordEncoder`의 `encode("원하는 비밀번호")`를 한 번 실행해 나온 해시 문자열을 위 SQL에 붙여넣습니다.

`seed.sql`(로컬/신규 환경용)도 함께 갱신:
```sql
-- [0. users 시드] X-User-Id: 1 임시 인증용 데모 계정. 없으면 플리 저장 시 FK 제약 위반 발생.
-- username: demo_user / password: demo1234 (BCrypt 해시는 애플리케이션 기동 시 생성 로직이 없으므로 미리 계산된 값을 사용)
INSERT INTO users (id, username, password_hash, nickname, created_at)
VALUES (1, 'demo_user', '<BCrypt 해시값>', 'demo_user', NOW());
```

**이 마이그레이션 SQL을 운영 DB에 직접 실행하는 것은 되돌리기 어려운 작업입니다. 반드시 사용자에게 먼저 보여주고 승인받은 뒤 실행하세요. 절대 임의로 실행하지 마세요.**

---

## 6. API 스펙 요약

### `POST /api/v1/auth/signup`

Request:
```json
{
  "username": "soundlover01",
  "password": "mypassword123",
  "nickname": "사운드러버"
}
```

Response (201 Created):
```json
{
  "userId": 2,
  "username": "soundlover01",
  "nickname": "사운드러버"
}
```

Error (409, username 중복):
```json
{
  "timestamp": "2026-08-19T12:00:00",
  "status": 409,
  "code": "DUPLICATE_USERNAME",
  "message": "이미 사용 중인 아이디입니다."
}
```

### `POST /api/v1/auth/login`

Request:
```json
{
  "username": "soundlover01",
  "password": "mypassword123"
}
```

Response (200 OK):
```json
{
  "userId": 2,
  "username": "soundlover01",
  "nickname": "사운드러버"
}
```

Error (401, 아이디 또는 비밀번호 불일치 — 둘 다 동일 메시지):
```json
{
  "timestamp": "2026-08-19T12:00:00",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

---

## 6-A. 프론트엔드 연동용 API 명세서

> 이 절은 프론트엔드 개발자(또는 프론트 담당 AI)가 이 문서만 보고 바로 연동 코드를 작성할 수 있도록,
> 기존 `README.md` 3절(API)과 동일한 포맷으로 작성했습니다. 백엔드 구현이 완료되면 이 절 내용을
> `README.md` 3절 표에도 추가해야 합니다 (체크리스트 9절 참고).

### Base URL

```
로컬:   http://localhost:8080/api/v1
운영:   http://1.201.116.122:8080/api/v1   (HTTP, HTTPS 아님 — 8/28 이후 서버 폐기 예정)
```

### 인증 관련 엔드포인트 요약

| 기능 | Method | Endpoint | 인증 | 성공 상태코드 |
| --- | --- | --- | --- | --- |
| 회원가입 | POST | `/auth/signup` | 불필요 | 201 Created |
| 로그인 | POST | `/auth/login` | 불필요 | 200 OK |

두 API 모두 `Content-Type: application/json` (파일 업로드 없음 — 기존 플레이리스트 API와 달리 멀티파트가 아닙니다).

이 두 API는 세션/쿠키/토큰을 전혀 발급하지 않습니다. 응답의 `userId`를 프론트가 직접 저장했다가, **이후 모든 인증 필요 API 호출 시 `X-User-Id` 헤더에 그대로 실어 보내야 합니다.** (플레이리스트 저장/조회 API와 동일한 기존 방식.)

### 6-A.1 회원가입 — `POST /auth/signup`

**요청**

```
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "username": "soundlover01",
  "password": "mypassword123",
  "nickname": "사운드러버"
}
```

| 필드 | 타입 | 필수 | 제약조건 |
| --- | --- | --- | --- |
| `username` | string | O | 4~20자, 영문/숫자/언더스코어(`_`)만 허용, 중복 불가 |
| `password` | string | O | 8~64자 |
| `nickname` | string | O | 1~50자, 화면에 표시되는 별명 (username과 별개 값) |

**성공 응답 (201 Created)**

```json
{
  "userId": 2,
  "username": "soundlover01",
  "nickname": "사운드러버"
}
```

**실패 응답**

| 상황 | HTTP 상태 | `code` | `message` |
| --- | --- | --- | --- |
| 필드 누락/형식 오류 (예: username 3자 이하, password 7자 이하) | 400 | `INVALID_REQUEST` | 필드별 상세 메시지 (예: `"password: password는 8~64자여야 합니다."`) |
| username 중복 | 409 | `DUPLICATE_USERNAME` | `"이미 사용 중인 아이디입니다."` |
| JSON 파싱 실패 (형식이 아예 깨진 경우) | 400 | `INVALID_REQUEST` | `"요청 값이 올바르지 않습니다."` |

프론트 UX 권장: username 입력 필드에 대해 blur 시점에 별도 "중복 확인" API는 제공하지 않습니다 (MVP 범위 아님) — 제출 시 409를 받으면 그 자리에서 "이미 사용 중인 아이디입니다" 에러 메시지를 인라인으로 보여주는 방식으로 처리하세요.

### 6-A.2 로그인 — `POST /auth/login`

**요청**

```
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "soundlover01",
  "password": "mypassword123"
}
```

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `username` | string | O |
| `password` | string | O |

**성공 응답 (200 OK)**

```json
{
  "userId": 2,
  "username": "soundlover01",
  "nickname": "사운드러버"
}
```

**실패 응답**

| 상황 | HTTP 상태 | `code` | `message` |
| --- | --- | --- | --- |
| username 미존재 **또는** 비밀번호 불일치 (구분 없음 — 보안상 의도된 동작) | 401 | `INVALID_CREDENTIALS` | `"아이디 또는 비밀번호가 올바르지 않습니다."` |
| 필드 누락 | 400 | `INVALID_REQUEST` | 필드별 상세 메시지 |

프론트 UX 권장: 401 응답은 "아이디를 잘못 입력했는지 비밀번호를 잘못 입력했는지" 백엔드가 알려주지 않으므로, 에러 메시지를 그대로 폼 하단에 표시하면 됩니다. 특정 입력 필드에만 에러를 표시하는 UI(예: 비밀번호 필드에만 빨간 테두리)는 만들지 마세요 — 어느 필드가 틀렸는지 백엔드가 구분해주지 않습니다.

### 6-A.3 로그인 이후 — 기존 API와의 연동 방법

로그인/회원가입 성공 응답의 `userId`를 프론트 상태(예: localStorage, Context, Zustand 등)에 저장한 뒤, 아래처럼 기존 플레이리스트 API 호출 시 헤더에 실어 보냅니다 (이 부분은 이미 구현되어 있던 기존 API 스펙 그대로이며, 변경사항 없음):

```
GET /api/v1/playlists
X-User-Id: 2
```

```
POST /api/v1/playlists
X-User-Id: 2
Content-Type: multipart/form-data
```

`X-User-Id` 헤더를 보내지 않으면 `401 USER_ID_REQUIRED`, 존재하지 않는 userId를 보내면 `401 INVALID_USER`가 반환됩니다 (기존 동작, 변경 없음).

로그아웃은 별도 API가 없습니다 — 프론트에서 저장해둔 `userId`를 지우기만 하면 됩니다 (서버에 세션이 없으므로 서버 측에서 할 일이 없습니다).

### 6-A.4 프론트 통합 테스트용 예시 (fetch)

```javascript
// 회원가입
const signupRes = await fetch(`${BASE_URL}/auth/signup`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username, password, nickname }),
});
if (!signupRes.ok) {
  const err = await signupRes.json();
  // err.code === "DUPLICATE_USERNAME" 등으로 분기
}
const { userId } = await signupRes.json();

// 로그인
const loginRes = await fetch(`${BASE_URL}/auth/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username, password }),
});
if (!loginRes.ok) {
  const err = await loginRes.json();
  // err.code === "INVALID_CREDENTIALS"
}
const { userId: loggedInUserId } = await loginRes.json();
// localStorage.setItem("userId", loggedInUserId) 등으로 저장 후
// 이후 모든 인증 필요 API 호출 시 headers: { "X-User-Id": String(loggedInUserId) }
```

---

## 7. 테스트 코드 (신규) — `src/test/java/com/soundscape/service/AuthServiceTest.java`

기존 `MusicRecommendationServiceTest`처럼 순수 단위 테스트(Mockito 기반, `UserRepository`/`PasswordEncoder`를 mock)로 작성합니다. 최소 아래 케이스를 커버하세요:

1. 정상 회원가입 → `AuthResponse`에 올바른 값이 담기는지
2. username 중복 → `BusinessException(DUPLICATE_USERNAME)` 발생 확인
3. 정상 로그인 → `AuthResponse` 반환 확인
4. 존재하지 않는 username 로그인 → `BusinessException(INVALID_CREDENTIALS)` 발생 확인
5. 비밀번호 불일치 로그인 → `BusinessException(INVALID_CREDENTIALS)` 발생 확인

`build.gradle`에 이미 `spring-boot-starter-webmvc-test`(Mockito 포함 추정)가 있으므로 별도 테스트 의존성 추가는 불필요합니다. 다만 실제로 Mockito가 classpath에 있는지 첫 테스트 작성 전에 확인하세요.

---

## 8. 프론트엔드 연동 참고 (구현 범위 아님, 참고용)

- 프론트는 백엔드 저장소와 분리되어 있고 팀원이 소유합니다. **이 문서를 기반으로 프론트 코드를 직접 수정하지 마세요.**
- 회원가입 성공 후 자동 로그인 처리(별도 `/login` 호출 없이 `/signup` 응답의 `userId`를 바로 사용) 또는 회원가입 후 로그인 페이지로 이동, 두 가지 UX 중 프론트 팀과 논의가 필요합니다.
- 로그인 성공 후 프론트는 `userId`를 로컬 저장(localStorage 등)해두고, 이후 모든 API 호출 시 `X-User-Id` 헤더에 실어 보내야 합니다 (기존 구조와 동일).
- CORS 설정(`WebConfig`)은 이미 존재하므로 추가 변경 불필요 — JSON POST 요청도 기존 허용 origin 정책을 그대로 따릅니다.

---

## 9. 플레이리스트 삭제 기능 — 구현 스펙

### 9-1. 현재 코드 상태 (baseline)

`PlaylistController`(`src/main/java/com/soundscape/controller/PlaylistController.java`)에는 저장/목록/상세만 있고 삭제가 없습니다:

```java
/** 1. 사진 기반 추천 (인증 불필요, IP 기반 rate limit 적용) */
@PostMapping(...) public RecommendationResponse recommendByPhoto(...) { ... }
/** 2. 플리 저장 (X-User-Id 필수) */
@PostMapping(value = "/playlists", ...) public PlaylistDetailResponse save(...) { ... }
/** 3. 플리 목록 조회 (X-User-Id 필수) */
@GetMapping("/playlists") public PlaylistListResponse findMyPlaylists(...) { ... }
/** 4. 플리 상세 조회 (X-User-Id 필수) */
@GetMapping("/playlists/{playlistId}") public PlaylistDetailResponse findDetail(...) { ... }
```

`PlaylistRepository`에는 소유권 확인용 조회(`findByIdAndUserIdWithItems`)만 있고, 삭제 관련 메서드가 없습니다. `S3Service`에는 업로드(`uploadCover`)/URL 발급(`presignedUrl`)만 있고 삭제(`deleteObject`)가 없습니다.

DB 스키마(`schema.sql`)의 `playlist_music` 테이블은 이미 `ON DELETE CASCADE`로 `playlist` FK가 걸려 있으므로(`fk_pm_playlist`), `playlist` 행을 지우면 그에 속한 `playlist_music` 행은 DB가 자동으로 지웁니다. **JPA 엔티티에서도 `Playlist.items`가 `cascade = CascadeType.ALL, orphanRemoval = true`로 이미 설정되어 있으므로, `playlistRepository.delete(playlist)` 한 줄이면 곡 매핑까지 함께 정리됩니다.** 별도 하위 삭제 로직이 필요 없습니다.

단, S3에 올라간 커버 이미지(`cover_object_key`)는 DB 삭제와 무관하게 별도로 지워야 합니다(안 지우면 S3에 고아 객체가 남지만, 버킷이 비공개 Presigned URL 방식이라 서비스 동작에는 지장 없음 — 용량 낭비 정도의 이슈).

### 9-2. `S3Service.java` 수정 — `deleteObject()` 추가

기존 파일 맨 아래(`resolveExtension` 메서드 앞)에 메서드를 추가합니다. import에 `software.amazon.awssdk.services.s3.model.DeleteObjectRequest`를 추가해야 합니다.

```java
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
```

```java
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
```

**설계 의도**: S3 삭제가 실패해도(네트워크 오류 등) 플레이리스트 DB 삭제 자체는 성공시킵니다. 사용자 입장에서 "삭제했는데 안 지워졌다"는 상황이 훨씬 나쁜 경험이므로, S3 정리는 best-effort로 처리하고 실패는 로그로만 남깁니다. `uploadCover`처럼 예외를 던지는 방식과 의도적으로 다르니 그대로 따르세요.

### 9-3. `PlaylistRepository.java` 수정 — 소유권 확인 조회 추가

삭제 시에는 곡 목록(`items`)까지 fetch join 할 필요가 없습니다(어차피 DB cascade가 지워줌). 커버 이미지 키만 있으면 되므로, 기존 `findByIdAndUserIdWithItems`를 재사용해도 되지만, 굳이 fetch join을 안 쓰는 가벼운 쿼리를 하나 추가하는 것을 권장합니다:

```java
@Query("select p from Playlist p where p.id = :playlistId and p.user.id = :userId")
Optional<Playlist> findByIdAndUserId(@Param("playlistId") Long playlistId, @Param("userId") Long userId);
```

(이미 있는 `findByIdAndUserIdWithItems`를 그대로 재사용해도 기능상 문제는 없습니다. 다만 fetch join으로 불필요하게 `playlist_music` 조인까지 가져오는 게 낭비이므로 가벼운 버전을 새로 추가하는 쪽을 권장합니다. 둘 중 하나만 선택해서 구현하세요 — 새 쿼리를 추가하면 4-3절 `PlaylistService.delete()` 예시 코드에서 `findByIdAndUserId`를 호출하고, 기존 걸 재사용하면 `findByIdAndUserIdWithItems`를 호출하도록 바꾸면 됩니다.)

### 9-4. `PlaylistService.java` 수정 — `delete()` 메서드 추가

`findDetail` 메서드 바로 아래에 추가합니다:

```java
/** 플리 삭제. 본인 소유가 아니거나 존재하지 않으면 404 (소유권 정보를 노출하지 않기 위해 403이 아닌 404로 통일). */
@Transactional
public void delete(Long userId, Long playlistId) {
    requireUser(userId);
    Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    s3Service.deleteObject(playlist.getCoverObjectKey());
    playlistRepository.delete(playlist);
}
```

**중요한 설계 결정 — 왜 403이 아니라 404인가**: 다른 사람의 `playlistId`를 넣었을 때 "존재는 하는데 권한이 없다(403)"고 알려주면, 공격자가 존재하는 playlistId 범위를 무작위로 스캔해서 추측할 수 있습니다. 이미 `findDetail`도 동일하게 `findByIdAndUserIdWithItems`가 실패하면 `PLAYLIST_NOT_FOUND`(404)로 통일하고 있으므로, 삭제도 같은 패턴을 따릅니다. **새 에러코드를 만들지 마세요** — 기존 `PLAYLIST_NOT_FOUND`를 그대로 재사용합니다.

### 9-5. `PlaylistController.java` 수정 — `DELETE /playlists/{playlistId}` 추가

`findDetail` 메서드 바로 아래에 추가합니다:

```java
/** 5. 플리 삭제 (X-User-Id 필수) */
@DeleteMapping("/playlists/{playlistId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(
        @RequestHeader(value = USER_HEADER, required = false) Long userId,
        @PathVariable Long playlistId) {
    playlistService.delete(userId, playlistId);
}
```

응답 바디 없이 `204 No Content`로 응답합니다 (REST 컨벤션 — 삭제 성공 시 반환할 리소스가 없으므로). `import org.springframework.web.bind.annotation.*`에 이미 `@DeleteMapping`이 포함되어 있으므로 import 추가는 필요 없습니다.

### 9-6. 에러 케이스 정리

| 케이스 | 처리 방식 | 응답 |
|---|---|---|
| `X-User-Id` 헤더 없음 | `requireUser`에서 체크 (기존 로직 재사용) | 401 `USER_ID_REQUIRED` |
| 존재하지 않는 userId | `requireUser`에서 체크 | 401 `INVALID_USER` |
| 존재하지 않는 playlistId, 또는 본인 소유가 아닌 playlistId | `findByIdAndUserId` 실패 | 404 `PLAYLIST_NOT_FOUND` (새 에러코드 없음, 기존 재사용) |
| playlistId가 숫자가 아닌 경로변수(예: `/playlists/abc`) | 기존 `GlobalExceptionHandler.handleMalformedRequest`(`MethodArgumentTypeMismatchException`)가 이미 처리 (수정 불필요) | 400 `INVALID_REQUEST` |
| 정상 삭제 | - | 204 No Content (바디 없음) |

새로 추가해야 하는 `ErrorCode`는 없습니다. 이 점이 회원가입/로그인 기능과의 차이입니다 — 삭제는 기존 에러코드 체계로 전부 커버됩니다.

---

## 9-A. 프론트엔드 연동용 API 명세서 — 플레이리스트 삭제

### `DELETE /api/v1/playlists/{playlistId}`

**요청**

```
DELETE /api/v1/playlists/10
X-User-Id: 2
```

- Path Variable: `playlistId` (number, 필수)
- Request Body: 없음
- Content-Type: 불필요 (바디가 없으므로)

**성공 응답**

```
204 No Content
```

바디가 없습니다. 프론트에서 `fetch` 사용 시 `response.json()`을 호출하면 파싱 에러가 나므로 호출하지 마세요 (상태 코드 `204`만 확인하면 됩니다).

**실패 응답**

| 상황 | HTTP 상태 | `code` | `message` |
| --- | --- | --- | --- |
| `X-User-Id` 헤더 누락 | 401 | `USER_ID_REQUIRED` | `"X-User-Id 헤더가 필요합니다."` |
| 존재하지 않는 사용자 | 401 | `INVALID_USER` | `"존재하지 않는 사용자입니다."` |
| 존재하지 않는 플레이리스트, **또는 다른 사람의 플레이리스트를 삭제하려는 경우** | 404 | `PLAYLIST_NOT_FOUND` | `"플레이리스트를 찾을 수 없습니다."` |

프론트 UX 권장:
- 삭제는 되돌릴 수 없는 작업이므로, API 호출 전에 반드시 확인 모달("정말 삭제하시겠습니까?")을 띄우세요.
- 404 응답이 와도 "권한이 없습니다" 같은 별도 메시지를 만들지 말고 "플레이리스트를 찾을 수 없습니다"로 그대로 표시하세요 (백엔드가 존재/권한 여부를 구분해서 알려주지 않는 의도된 설계입니다 — 9-4절 참고).
- 삭제 성공(204) 시 프론트는 목록 화면에서 해당 항목을 로컬 상태에서 제거하거나, `GET /playlists`를 재호출해 목록을 새로고침하세요.

**fetch 예시**

```javascript
const res = await fetch(`${BASE_URL}/playlists/${playlistId}`, {
  method: "DELETE",
  headers: { "X-User-Id": String(userId) },
});

if (res.status === 204) {
  // 성공: 로컬 상태에서 해당 플레이리스트 제거
} else {
  const err = await res.json();
  // err.code === "PLAYLIST_NOT_FOUND" 등으로 분기
}
```

---

## 10. 체크리스트 (구현 완료 후 확인)

- [ ] `build.gradle`에 `spring-security-crypto` 추가, 빌드 성공 확인
- [ ] `PasswordConfig` 빈 등록 확인 (`BCryptPasswordEncoder`)
- [ ] `User` 엔티티에 `username`, `passwordHash` 필드 및 `create()` 정적 팩토리 추가
- [ ] `UserRepository`에 `existsByUsername`, `findByUsername` 추가
- [ ] `ErrorCode`에 `DUPLICATE_USERNAME`, `INVALID_CREDENTIALS`, `INVALID_PASSWORD_FORMAT` 추가
- [ ] `SignupRequest`, `LoginRequest`, `AuthResponse` DTO 작성
- [ ] `AuthService.signup()`, `AuthService.login()` 구현
- [ ] `AuthController`에 `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` 구현
- [ ] `schema.sql` 수정 (로컬/신규 환경용)
- [ ] `seed.sql` 수정 (demo_user에 username/password 부여)
- [ ] **운영 DB 마이그레이션 SQL은 사용자 승인 후 별도 실행** (5절, 직접 실행 금지 — 문서 작성/제안까지만)
- [ ] `AuthServiceTest` 작성, 5개 케이스 통과 확인
- [ ] `README.md`에 새 API 스펙 섹션 추가 (기존 API 문서화 스타일 참고: 요청/응답 예시 + 에러코드 표)
- [ ] `./gradlew test` 전체 통과 확인
- [ ] 로컬에서 `curl`로 signup → login 실제 호출 테스트 (아래 예시)

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser01","password":"testpass123","nickname":"테스트유저"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser01","password":"testpass123"}'
```

**플레이리스트 삭제 기능 체크리스트**

- [ ] `S3Service`에 `deleteObject()` 추가, import (`DeleteObjectRequest`) 확인
- [ ] `PlaylistRepository`에 `findByIdAndUserId` (또는 기존 `findByIdAndUserIdWithItems` 재사용) 추가/확인
- [ ] `PlaylistService.delete()` 구현 — S3 삭제는 best-effort(예외를 던지지 않음), DB 삭제는 `playlistRepository.delete(playlist)`로 cascade 정리
- [ ] `PlaylistController`에 `DELETE /api/v1/playlists/{playlistId}` 추가, `204 No Content` 응답 확인
- [ ] 새 `ErrorCode`를 추가하지 않았는지 확인 (기존 `PLAYLIST_NOT_FOUND` 재사용이 맞는 설계)
- [ ] `PlaylistServiceDeleteTest` 작성 — 정상 삭제, 존재하지 않는 playlistId, 타인 소유 playlistId(404로 통일되는지) 케이스 확인
- [ ] `README.md`에 삭제 API 스펙 추가 (기존 표에 5번째 행으로)
- [ ] 로컬에서 `curl`로 삭제 실제 호출 테스트

```bash
curl -X DELETE http://localhost:8080/api/v1/playlists/1 \
  -H "X-User-Id: 1" -i
# 204 No Content 확인 후, 같은 요청 재호출 시 404 PLAYLIST_NOT_FOUND 확인
```
