# 📦 SoundScape 백엔드 전체 소스 코드 아카이브 (39개 파일)

> **[문서 목적]**
> IntelliJ 내부 AI(Claude Code)가 이 문서 하나만 참조해도 SoundScape 백엔드 프로젝트를 **완전히 복원하고 이어서 개발**할 수 있도록, 작성 완료된 39개 파일(Java 34개 + 설정·SQL·문서 5개)의 전체 소스를 경로와 함께 정리한 아카이브입니다.
>
> **기술 스택: Spring Boot 4.0.7 / Spring Framework 7 / Jakarta EE 11 / Jackson 3 / Hibernate 7 / Java 21**
>
> 설계 배경·API 명세·알고리즘 근거는 별도 문서 `soundscape_handover_v2.md` 를 참조하십시오. 본 문서는 **코드 원문**만 담습니다.

---

## 🛑 [필수 소통 지침] 답변 방식 규칙

1. **쓸데없는 칭찬, 감정적 감탄사, 미사여구(수식어) 전면 배제.**
2. **요청한 질문 및 작업에 대해 간결하고 명확한 핵심 대답만 전달.**
3. **자체 피드백을 거친 더 효율적인 개발 방법 및 개발 시간을 절약할 수 있는 실행 방법 중심으로 답변.**

---

## ⚙️ 사용법 (Claude Code 지시 예시)

```
HANDOVER.md 와 SOURCE.md 를 읽고,
SOURCE.md 에 정의된 파일들을 각 경로 그대로 생성해줘.
단 SoundscapeApplication.java 는 이미 존재하면 건너뛰고,
build.gradle 은 기존 파일을 덮어쓰지 말고 dependencies 블록에
누락된 3줄(validation, awssdk bom, s3)만 추가한 뒤
spring-boot-starter-web 을 spring-boot-starter-webmvc 로 바꿔줘.
```

---

## 🚨 코드 복원 시 절대 규칙 8가지

1. **패키지명은 `com.soundscape`** — 파일 상단 `package` 선언을 임의로 바꾸지 말 것.
2. **`Music.moodVector()` 와 `PhotoAnalysisResult.moodVector()` 의 배열 순서는 반드시 동일** — `brightness, energy, calmness, warmth, positivity, dreaminess`. 어긋나면 추천이 조용히 망가진다.
3. **`PlaylistTheme` enum 값과 `OpenAiVisionService` 의 json_schema enum 은 1:1 일치** — 테마 추가 시 양쪽을 함께 수정.
4. **`seed.sql` 의 `audio_object_key` 와 S3 오브젝트 키는 글자 하나까지 일치** — 불일치 시 Presigned URL 은 발급되나 재생 시 403.
5. **`spring.jpa.hibernate.ddl-auto` 는 `none` 유지** — 스키마는 `schema.sql` 로만 관리한다.
6. **Jackson import 를 일괄 치환하지 말 것** — `databind`/`core` 는 `tools.jackson`, **애노테이션은 `com.fasterxml.jackson.annotation` 그대로**.
7. **`JsonNode` 순회 코드를 새로 도입하지 말 것** — Jackson 3 에서 일부 메서드가 리네임되었다. record 역직렬화 방식을 유지한다.
8. **`RestClient.body()` 는 항상 null 검사** — Boot 4 의 JSpecify 로 nullable 이 명시되었다.

---

## ⚠️ Spring Boot 3 → 4 차이 요약 (Boot 3 예제를 참조하기 전에 반드시 대조)

| 영역 | Boot 3 | Boot 4 (현재 코드) |
| :--- | :--- | :--- |
| 웹 스타터 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Jackson databind | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.json.JsonMapper` |
| Jackson 애노테이션 | `com.fasterxml.jackson.annotation.*` | **변경 없음** |
| HTTP 타임아웃 | `spring.http.client.*` | `spring.http.clients.*` |
| RestClient 생성 | `RestClient.builder()` + RequestFactory | 오토컨피그된 `RestClient.Builder` 주입 |
| Hibernate | 6.x, dialect 명시 | 7.x, dialect 자동 감지 |
| 테스트 | JUnit 4 전이 포함 | `junit-platform-launcher` 를 `testRuntimeOnly` 명시 |
| Java / 서블릿 | 17 / Servlet 6.0 | 21 권장 / Servlet 6.1 |

---

## 📑 파일 목록

| # | 경로 |
| :--- | :--- |
| 1 | `build.gradle` |
| 2 | `src/main/resources/application.properties` |
| 3 | `.gitignore` |
| 4 | `src/main/resources/db/schema.sql` |
| 5 | `src/main/resources/db/seed.sql` |
| 6 | `src/main/java/com/soundscape/SoundscapeApplication.java` |
| 7 | `src/main/java/com/soundscape/enums/Genre.java` |
| 8 | `src/main/java/com/soundscape/enums/PlaylistTheme.java` |
| 9 | `src/main/java/com/soundscape/enums/SourceType.java` |
| 10 | `src/main/java/com/soundscape/domain/User.java` |
| 11 | `src/main/java/com/soundscape/domain/Music.java` |
| 12 | `src/main/java/com/soundscape/domain/Playlist.java` |
| 13 | `src/main/java/com/soundscape/domain/PlaylistMusic.java` |
| 14 | `src/main/java/com/soundscape/repository/UserRepository.java` |
| 15 | `src/main/java/com/soundscape/repository/MusicRepository.java` |
| 16 | `src/main/java/com/soundscape/repository/PlaylistRepository.java` |
| 17 | `src/main/java/com/soundscape/dto/PhotoAnalysisResult.java` |
| 18 | `src/main/java/com/soundscape/dto/MusicResponse.java` |
| 19 | `src/main/java/com/soundscape/dto/RecommendationResponse.java` |
| 20 | `src/main/java/com/soundscape/dto/GenreRecommendationRequest.java` |
| 21 | `src/main/java/com/soundscape/dto/PlaylistSaveRequest.java` |
| 22 | `src/main/java/com/soundscape/dto/PlaylistSummaryResponse.java` |
| 23 | `src/main/java/com/soundscape/dto/PlaylistDetailResponse.java` |
| 24 | `src/main/java/com/soundscape/exception/ErrorCode.java` |
| 25 | `src/main/java/com/soundscape/exception/BusinessException.java` |
| 26 | `src/main/java/com/soundscape/exception/ErrorResponse.java` |
| 27 | `src/main/java/com/soundscape/exception/GlobalExceptionHandler.java` |
| 28 | `src/main/java/com/soundscape/config/WebConfig.java` |
| 29 | `src/main/java/com/soundscape/config/AwsS3Config.java` |
| 30 | `src/main/java/com/soundscape/config/OpenAiConfig.java` |
| 31 | `src/main/java/com/soundscape/service/MusicCacheService.java` |
| 32 | `src/main/java/com/soundscape/service/MusicRecommendationService.java` |
| 33 | `src/main/java/com/soundscape/service/OpenAiVisionService.java` |
| 34 | `src/main/java/com/soundscape/service/S3Service.java` |
| 35 | `src/main/java/com/soundscape/service/PlaylistService.java` |
| 36 | `src/main/java/com/soundscape/controller/PlaylistController.java` |
| 37 | `src/main/java/com/soundscape/controller/AdminController.java` |
| 38 | `src/test/java/com/soundscape/service/MusicRecommendationServiceTest.java` |
| 39 | `README.md` |

---

# 빌드 및 설정

## 1. `build.gradle`
> **[2026-08-10 실제 코드로 갱신]** Initializr 다운로드본 기준 Java 툴체인은 17로 그대로 두었다(21로 올리지 않아도 Boot 4.0.7 기동/컴파일에 문제 없음, 확인됨). `spring-boot-starter-restclient` 가 추가로 필요하다 — 자세한 사유는 트러블슈팅 표 참조. 테스트 의존성은 Initializr 가 생성한 대로 `spring-boot-starter-data-jpa-test` / `spring-boot-starter-webmvc-test` 조합을 사용 중이며, 이 조합도 `spring-boot-starter-test` 와 동등하게 JUnit5/Mockito 를 가져온다(문제 없음).

````gradle
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
````

## 2. `src/main/resources/application.properties`
> **[2026-08-13 실제 코드로 갱신]** Boot 4 신규 네임스페이스 spring.http.clients.* 사용. 콜론 뒤가 기본값이므로 환경변수 미사용 시 이 자리를 직접 수정. 아래는 현재 이 환경에서 실제로 적용 중인 값이다 — `root` 계정 인증 실패로 전용 계정(`soundscape`/`soundscape1234`)으로 전환했다. `aws.credentials.*` / `openai.api-key` 는 실제 발급받은 키로 채워져 있어 AWS S3 · OpenAI Vision 연동이 정상 동작 중이나, **이 문서에는 보안상 실제 키를 적지 않고 마스킹 처리**했다. 실제 배포 시에는 이 자리(콜론 뒤 기본값)가 아니라 환경변수로 진짜 값을 주입할 것을 권장한다.

````properties
spring.application.name=soundscape

# ---------- MySQL ----------
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/soundscape?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=soundscape
spring.datasource.password=soundscape1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# schema.sql / seed.sql 로 직접 관리. JPA 자동 DDL 사용 안 함
# Hibernate 7 은 dialect 를 자동 감지하므로 명시하지 않는다.
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.show-sql=true

# ---------- Multipart (사진 업로드) ----------
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=20MB

# ---------- HTTP Client (Spring Boot 4 신규 네임스페이스) ----------
# 구 spring.http.client.* 는 deprecated. OpenAI 호출 타임아웃이 여기서 적용된다.
spring.http.clients.connect-timeout=5s
spring.http.clients.read-timeout=20s

# ---------- AWS S3 ----------
aws.s3.bucket=soundscape-bucket-sangjin
aws.s3.region=ap-northeast-2
aws.credentials.access-key=${AWS_ACCESS_KEY:***MASKED_AWS_ACCESS_KEY***}
aws.credentials.secret-key=${AWS_SECRET_KEY:***MASKED_AWS_SECRET_KEY***}
aws.s3.presign-minutes=60

# ---------- OpenAI ----------
openai.api-key=${OPENAI_API_KEY:***MASKED_OPENAI_API_KEY***}
openai.model=gpt-4o
openai.base-url=https://api.openai.com/v1
openai.image-detail=low
openai.image-max-size=512

# ---------- 서버 ----------
server.port=8080
java.awt.headless=true
````

## 3. `.gitignore`
> 빌드 산출물 및 키 파일 제외.

````gitignore
.gradle/
build/
out/
.idea/
*.iml
.env
````

# DB 스크립트

## 4. `src/main/resources/db/schema.sql`
> MySQL 8 DDL. seed.sql보다 먼저 실행할 것.

````sql
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
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
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
````

## 5. `src/main/resources/db/seed.sql`
> users 데모 계정 1행 + music 26곡 + music_theme 75행.

````sql
-- ============================================================
-- SoundScape 시드 데이터 (users 1명 + music 26곡 + music_theme 다중매핑)
-- 실행 순서: schema.sql -> seed.sql
-- ============================================================
USE soundscape;

-- [0. users 시드] X-User-Id: 1 임시 인증용 데모 계정. 없으면 플리 저장 시 FK 제약 위반 발생.
INSERT INTO users (id, nickname, created_at) VALUES (1, 'demo_user', NOW());

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
(13, 'Natural Breeze', 'HarumachiMusic', 'music/natural_breeze.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.72, 0.41, 0.81, 0.78, 0.79, 0.58, 220, NOW(), NOW()),
(14, 'escape from the rain', 'HarumachiMusic', 'music/escape_rain.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.44, 0.26, 0.89, 0.91, 0.67, 0.83, 139, NOW(), NOW()),
(15, 'a rustic cottage', 'HarumachiMusic', 'music/rustic_cottage.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.68, 0.33, 0.85, 0.88, 0.76, 0.61, 146, NOW(), NOW()),
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
(13, 'REST_HEALING'), (13, 'WALK_DRIVE_TRAVEL'), (13, 'BRIGHT_DAY'),
(14, 'RAINY_DAY'), (14, 'REST_HEALING'), (14, 'WALK_DRIVE_TRAVEL'),
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
````

# 애플리케이션 진입점

## 6. `src/main/java/com/soundscape/SoundscapeApplication.java`
> Initializr 생성본과 동일. 이미 존재하면 덮어쓰지 않아도 된다.

````java
package com.soundscape;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SoundscapeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SoundscapeApplication.class, args);
    }
}
````

# enums

## 7. `src/main/java/com/soundscape/enums/Genre.java`
> 7대 장르. 0곡 장르도 Enum은 유지하며 요청 시 404로 응답한다.

````java
package com.soundscape.enums;

/**
 * 7대 장르 표준 Enum.
 * 현재 DB 보유 곡수: LOFI_INSTRUMENTAL(8), JAZZ_CLASSICAL_NEWAGE(10), POP(8).
 * HIPHOP_RAP / RNB_SOUL / KPOP / JPOP_ANIME_OST 는 음원 추가 수급 예정 (현재 0곡).
 * 0곡 장르 요청 시 GENRE_NOT_AVAILABLE(404) 로 응답한다.
 */
public enum Genre {
    LOFI_INSTRUMENTAL,
    JAZZ_CLASSICAL_NEWAGE,
    POP,
    HIPHOP_RAP,
    RNB_SOUL,
    KPOP,
    JPOP_ANIME_OST
}
````

## 8. `src/main/java/com/soundscape/enums/PlaylistTheme.java`
> 7대 테마. OpenAiVisionService의 json_schema enum 값과 1:1 일치해야 한다.

````java
package com.soundscape.enums;

/** 7대 테마 표준 Enum. OpenAiVisionService 의 json_schema enum 값과 1:1 일치해야 한다. */
public enum PlaylistTheme {
    STUDY_FOCUS,
    REST_HEALING,
    WALK_DRIVE_TRAVEL,
    RAINY_DAY,
    NIGHT_DREAMY,
    BRIGHT_DAY,
    PARTY_ENERGY
}
````

## 9. `src/main/java/com/soundscape/enums/SourceType.java`
> 플레이리스트 생성 경로.

````java
package com.soundscape.enums;

/** 플레이리스트 생성 경로. PHOTO=사진 기반 추천, GENRE=장르 기반 추천 */
public enum SourceType {
    PHOTO,
    GENRE
}
````

# domain (JPA Entity)

## 10. `src/main/java/com/soundscape/domain/User.java`
> 임시 인증용. seed.sql의 id=1 demo_user와 대응.

````java
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
````

## 11. `src/main/java/com/soundscape/domain/Music.java`
> 6대 감정 수치 + @ElementCollection 테마. moodVector() 인덱스 순서가 PhotoAnalysisResult와 반드시 동일해야 한다.

````java
package com.soundscape.domain;

import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 음원 엔티티. 6대 감정 수치(0.0~1.0)와 다중 테마를 보유한다.
 * music_theme 는 별도 Entity 대신 @ElementCollection 으로 매핑하여 복합키 Entity/Repository 작성을 제거했다.
 */
@Entity
@Table(name = "music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Music {

    /** 감정 벡터 차원 수. moodScore 정규화(sqrt(6))의 기준. */
    public static final int MOOD_DIMENSION = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    @Column(name = "audio_object_key", nullable = false, length = 500)
    private String audioObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Genre genre;

    @Column(nullable = false)
    private double brightness;

    @Column(nullable = false)
    private double energy;

    @Column(nullable = false)
    private double calmness;

    @Column(nullable = false)
    private double warmth;

    @Column(nullable = false)
    private double positivity;

    @Column(nullable = false)
    private double dreaminess;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "music_theme", joinColumns = @JoinColumn(name = "music_id"))
    @Column(name = "theme", length = 40)
    @Enumerated(EnumType.STRING)
    private Set<PlaylistTheme> themes = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 6대 감정 벡터. 유클리드 거리 계산 시 photo 벡터와 인덱스 순서가 반드시 일치해야 한다. */
    public double[] moodVector() {
        return new double[]{brightness, energy, calmness, warmth, positivity, dreaminess};
    }
}
````

## 12. `src/main/java/com/soundscape/domain/Playlist.java`
> replaceItems()가 musicIds 순서를 그대로 position 0..n으로 저장한다.

````java
package com.soundscape.domain;

import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_genre", length = 40)
    private Genre sourceGenre;

    @Column(name = "cover_object_key", length = 500)
    private String coverObjectKey;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<PlaylistMusic> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Playlist(User user, String title, SourceType sourceType, Genre sourceGenre, String coverObjectKey) {
        this.user = user;
        this.title = title;
        this.sourceType = sourceType;
        this.sourceGenre = sourceGenre;
        this.coverObjectKey = coverObjectKey;
    }

    public static Playlist create(User user, String title, SourceType sourceType, Genre sourceGenre, String coverObjectKey) {
        return new Playlist(user, title, sourceType, sourceGenre, coverObjectKey);
    }

    /** musicIds 순서를 그대로 position(0..n)으로 저장한다. */
    public void replaceItems(List<Long> musicIds) {
        this.items.clear();
        for (int i = 0; i < musicIds.size(); i++) {
            this.items.add(PlaylistMusic.of(this, musicIds.get(i), i));
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
````

## 13. `src/main/java/com/soundscape/domain/PlaylistMusic.java`
> music을 엔티티로 연관하지 않고 musicId(Long)만 저장. 조회 시 캐시에서 해석하므로 music 테이블 조인이 없다.

````java
package com.soundscape.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 플레이리스트 - 곡 매핑. music 은 엔티티 연관 대신 musicId(Long)만 저장한다.
 * 조회 시 MusicCacheService 인메모리 캐시에서 즉시 해석하므로 music 테이블 조인이 발생하지 않는다.
 */
@Entity
@Table(name = "playlist_music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Column(name = "music_id", nullable = false)
    private Long musicId;

    @Column(name = "`position`", nullable = false)
    private int position;

    private PlaylistMusic(Playlist playlist, Long musicId, int position) {
        this.playlist = playlist;
        this.musicId = musicId;
        this.position = position;
    }

    static PlaylistMusic of(Playlist playlist, Long musicId, int position) {
        return new PlaylistMusic(playlist, musicId, position);
    }
}
````

# repository

## 14. `src/main/java/com/soundscape/repository/UserRepository.java`

````java
package com.soundscape.repository;

import com.soundscape.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
````

## 15. `src/main/java/com/soundscape/repository/MusicRepository.java`
> findAllWithThemes()가 fetch join으로 N+1을 제거한다. 기동 시 1회만 호출.

````java
package com.soundscape.repository;

import com.soundscape.domain.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MusicRepository extends JpaRepository<Music, Long> {

    /** 서버 기동 시 1회 호출. themes 를 fetch join 하여 N+1 없이 26곡 전량을 한 번에 로드한다. */
    @Query("select distinct m from Music m left join fetch m.themes order by m.id asc")
    List<Music> findAllWithThemes();
}
````

## 16. `src/main/java/com/soundscape/repository/PlaylistRepository.java`
> 목록/상세 모두 items를 fetch join하여 LazyInitializationException을 방지한다.

````java
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
}
````

# dto

## 17. `src/main/java/com/soundscape/dto/PhotoAnalysisResult.java`
> GPT-4o Vision 응답 매핑. Jackson 3에서도 애노테이션 import는 com.fasterxml.jackson.annotation 유지.

````java
package com.soundscape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soundscape.enums.PlaylistTheme;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GPT-4o Vision 이 반환하는 사진 감성 분석 결과.
 * json_schema strict 모드로 6개 수치 + themes 가 항상 채워져 온다.
 *
 * [Jackson 3 주의] databind 는 tools.jackson 으로 이동했지만
 * 애노테이션은 그대로 com.fasterxml.jackson.annotation 을 사용한다. import 를 바꾸지 말 것.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoAnalysisResult(
        double brightness,
        double energy,
        double calmness,
        double warmth,
        double positivity,
        double dreaminess,
        List<PlaylistTheme> themes
) {
    /** Music.moodVector() 와 인덱스 순서가 반드시 동일해야 한다. */
    public double[] moodVector() {
        return new double[]{brightness, energy, calmness, warmth, positivity, dreaminess};
    }

    public Set<PlaylistTheme> themeSet() {
        return themes == null ? Set.of() : new LinkedHashSet<>(themes);
    }

    /** 모델이 범위를 벗어난 값을 주는 경우를 대비한 0.0~1.0 클램핑. */
    public PhotoAnalysisResult clamped() {
        return new PhotoAnalysisResult(
                clamp(brightness), clamp(energy), clamp(calmness),
                clamp(warmth), clamp(positivity), clamp(dreaminess),
                themes
        );
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
````

## 18. `src/main/java/com/soundscape/dto/MusicResponse.java`
> audioUrl은 S3 Presigned URL(기본 60분).

````java
package com.soundscape.dto;

import com.soundscape.domain.Music;
import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;

import java.util.Set;

/** 곡 단건 응답. audioUrl 은 S3 Presigned URL(기본 60분). */
public record MusicResponse(
        Long musicId,
        String title,
        String artist,
        Genre genre,
        int durationSeconds,
        Set<PlaylistTheme> themes,
        String audioUrl,
        Double score
) {
    public static MusicResponse of(Music music, String audioUrl, Double score) {
        return new MusicResponse(
                music.getId(),
                music.getTitle(),
                music.getArtist(),
                music.getGenre(),
                music.getDurationSeconds(),
                music.getThemes(),
                audioUrl,
                score
        );
    }
}
````

## 19. `src/main/java/com/soundscape/dto/RecommendationResponse.java`
> 저장 전 상태이므로 playlistId가 없다.

````java
package com.soundscape.dto;

import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.enums.SourceType;

import java.util.List;
import java.util.Set;

/** 추천 결과 응답. 저장 전 상태이므로 playlistId 는 없다. */
public record RecommendationResponse(
        SourceType sourceType,
        Genre sourceGenre,
        PhotoAnalysisResult analyzedMood,
        Set<PlaylistTheme> analyzedThemes,
        List<MusicResponse> musics
) {
    public static RecommendationResponse photo(PhotoAnalysisResult mood, List<MusicResponse> musics) {
        return new RecommendationResponse(SourceType.PHOTO, null, mood, mood.themeSet(), musics);
    }

    public static RecommendationResponse genre(Genre genre, List<MusicResponse> musics) {
        return new RecommendationResponse(SourceType.GENRE, genre, null, Set.of(), musics);
    }
}
````

## 20. `src/main/java/com/soundscape/dto/GenreRecommendationRequest.java`

````java
package com.soundscape.dto;

import com.soundscape.enums.Genre;
import jakarta.validation.constraints.NotNull;

/** POST /playlists/recommendations/genre 요청 바디: {"genre": "KPOP"} */
public record GenreRecommendationRequest(
        @NotNull(message = "genre는 필수입니다.")
        Genre genre
) {
}
````

## 21. `src/main/java/com/soundscape/dto/PlaylistSaveRequest.java`
> JSON part가 아닌 일반 폼 필드로 바인딩된다. 프론트는 FormData에 평문 필드로 append.

````java
package com.soundscape.dto;

import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * POST /playlists (multipart/form-data) 폼 필드 바인딩 DTO.
 * 프론트는 FormData 에 photo(File), title, sourceType, sourceGenre, musicIds 를 담아 전송한다.
 * musicIds 는 append('musicIds', id) 를 5회 반복하거나 'musicIds=1,2,3,4,5' 형태 모두 허용된다.
 */
@Getter
@Setter
public class PlaylistSaveRequest {

    @NotNull(message = "title은 필수입니다.")
    private String title;

    @NotNull(message = "sourceType은 필수입니다.")
    private SourceType sourceType;

    /** sourceType=GENRE 인 경우에만 사용. */
    private Genre sourceGenre;

    @NotEmpty(message = "musicIds는 1개 이상이어야 합니다.")
    private List<Long> musicIds;
}
````

## 22. `src/main/java/com/soundscape/dto/PlaylistSummaryResponse.java`

````java
package com.soundscape.dto;

import com.soundscape.domain.Playlist;
import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;

/** GET /playlists 목록 항목. coverUrl 은 S3 Presigned URL. */
public record PlaylistSummaryResponse(
        Long playlistId,
        String title,
        SourceType sourceType,
        Genre sourceGenre,
        String coverUrl,
        int musicCount,
        String createdAt
) {
    public static PlaylistSummaryResponse of(Playlist playlist, String coverUrl) {
        return new PlaylistSummaryResponse(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getSourceType(),
                playlist.getSourceGenre(),
                coverUrl,
                playlist.getItems().size(),
                playlist.getCreatedAt().toString()
        );
    }
}
````

## 23. `src/main/java/com/soundscape/dto/PlaylistDetailResponse.java`

````java
package com.soundscape.dto;

import com.soundscape.domain.Playlist;
import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;

import java.util.List;

/** GET /playlists/{playlistId} 상세. 조회 시점에 오디오 Presigned URL 을 재발급한다. */
public record PlaylistDetailResponse(
        Long playlistId,
        String title,
        SourceType sourceType,
        Genre sourceGenre,
        String coverUrl,
        String createdAt,
        List<MusicResponse> musics
) {
    public static PlaylistDetailResponse of(Playlist playlist, String coverUrl, List<MusicResponse> musics) {
        return new PlaylistDetailResponse(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getSourceType(),
                playlist.getSourceGenre(),
                coverUrl,
                playlist.getCreatedAt().toString(),
                musics
        );
    }
}
````

# exception

## 24. `src/main/java/com/soundscape/exception/ErrorCode.java`
> 에러 코드 단일 정의 지점. 신규 예외는 여기에 추가한다.

````java
package com.soundscape.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "X-User-Id 헤더가 필요합니다."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),

    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "이미지 파일을 읽을 수 없습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "파일이 비어 있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    INVALID_GENRE(HttpStatus.BAD_REQUEST, "INVALID_GENRE", "지원하지 않는 장르입니다."),
    INVALID_MUSIC_IDS(HttpStatus.BAD_REQUEST, "INVALID_MUSIC_IDS", "존재하지 않는 musicId가 포함되어 있습니다."),

    GENRE_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "GENRE_NOT_AVAILABLE", "해당 장르의 음원이 아직 준비되지 않았습니다."),
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND", "플레이리스트를 찾을 수 없습니다."),

    VISION_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "VISION_ANALYSIS_FAILED", "사진 분석에 실패했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "S3_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
````

## 25. `src/main/java/com/soundscape/exception/BusinessException.java`

````java
package com.soundscape.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
````

## 26. `src/main/java/com/soundscape/exception/ErrorResponse.java`
> 공통 에러 응답 DTO.

````java
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
````

## 27. `src/main/java/com/soundscape/exception/GlobalExceptionHandler.java`
> BindException 하나로 @RequestBody/@ModelAttribute 검증 실패를 모두 처리한다.

````java
package com.soundscape.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", code.getCode(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    /** @Valid @RequestBody -> MethodArgumentNotValidException, @Valid @ModelAttribute -> BindException. 상위 타입으로 한 번에 처리. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("[Unexpected]", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, null));
    }
}
````

# config

## 28. `src/main/java/com/soundscape/config/WebConfig.java`
> 시연용 CORS 전면 허용. 배포 시 도메인을 좁힐 것.

````java
package com.soundscape.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 시연 단계 CORS 전면 허용. 배포 시 allowedOriginPatterns 를 프론트 도메인으로 좁힐 것. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
````

## 29. `src/main/java/com/soundscape/config/AwsS3Config.java`
> S3Client와 S3Presigner 빈 등록.

````java
package com.soundscape.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
````

## 30. `src/main/java/com/soundscape/config/OpenAiConfig.java`
> Boot 4 오토컨피그된 RestClient.Builder를 주입받는다. 타임아웃은 spring.http.clients.* 에서 설정.

````java
package com.soundscape.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 전용 RestClient.
 * Spring Boot 4 가 오토컨피그한 RestClient.Builder 를 주입받아 사용한다.
 * 커넥트/리드 타임아웃은 application.properties 의 spring.http.clients.* 에서 설정한다
 * (Boot 4 에서 spring.http.client.* 는 deprecated).
 */
@Configuration
public class OpenAiConfig {

    @Bean(name = "openAiRestClient")
    public RestClient openAiRestClient(
            RestClient.Builder builder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
````

# service

## 31. `src/main/java/com/soundscape/service/MusicCacheService.java`
> @PostConstruct 인메모리 전량 적재. 추천 시 DB 접근 0회. reload()로 무중단 갱신.

````java
package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.enums.Genre;
import com.soundscape.repository.MusicRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 전체 음원 인메모리 캐시.
 * 곡 수가 수십~수천 규모라 매 추천 요청마다 DB를 조회할 이유가 없다.
 * 기동 시 1회 전량 로드하고, 이후 추천/조회는 전부 메모리 연산으로 처리한다(응답 수 ms).
 * 음원 추가 수급 후에는 POST /api/v1/admin/music/reload 로 재적재하면 서버 재시작이 불필요하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicCacheService {

    private final MusicRepository musicRepository;

    private volatile List<Music> allMusics = List.of();
    private volatile Map<Long, Music> musicById = Map.of();
    private volatile Map<Genre, List<Music>> musicByGenre = Map.of();

    @PostConstruct
    public void load() {
        List<Music> loaded = List.copyOf(musicRepository.findAllWithThemes());

        this.allMusics = loaded;
        this.musicById = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(Music::getId, Function.identity()));

        Map<Genre, List<Music>> byGenre = new LinkedHashMap<>();
        for (Genre genre : Genre.values()) {
            byGenre.put(genre, loaded.stream()
                    .filter(m -> m.getGenre() == genre)
                    .toList());
        }
        this.musicByGenre = Map.copyOf(byGenre);

        log.info("[MusicCache] 음원 {}곡 적재 완료. 장르별 보유: {}", loaded.size(),
                musicByGenre.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue().size())
                        .collect(Collectors.joining(", ")));
    }

    /** 음원 INSERT 후 캐시 재적재. */
    public int reload() {
        load();
        return allMusics.size();
    }

    public List<Music> findAll() {
        return allMusics;
    }

    public List<Music> findByGenre(Genre genre) {
        return musicByGenre.getOrDefault(genre, List.of());
    }

    public Music findById(Long musicId) {
        return musicById.get(musicId);
    }

    public boolean existsAll(List<Long> musicIds) {
        return musicIds.stream().allMatch(musicById::containsKey);
    }
}
````

## 32. `src/main/java/com/soundscape/service/MusicRecommendationService.java`
> 핵심 알고리즘. Final Score = moodScore*0.70 + themeScore*0.30, moodScore = 1 - (d/√6).

````java
package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.dto.PhotoAnalysisResult;
import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 가중치 기반 추천 알고리즘.
 *
 *   Final Score = (moodScore * 0.70) + (themeScore * 0.30)
 *
 *   moodScore  : 사진 6대 감정 벡터와 곡 6대 감정 벡터의 유클리드 거리 d 를
 *                최대 거리 sqrt(6) 으로 나눠 정규화한 유사도. moodScore = 1 - (d / sqrt(6)).
 *                결과는 항상 0.0 ~ 1.0 이며 거리 0 에서의 무한대 폭주가 발생하지 않는다.
 *   themeScore : 사진 분석 테마와 곡 테마의 교집합 비율. (매칭 수 / 사진 테마 수)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicRecommendationService {

    /** 6차원 단위 초입방체의 최대 유클리드 거리 = sqrt(6) ≈ 2.4495 */
    private static final double MAX_DISTANCE = Math.sqrt(Music.MOOD_DIMENSION);

    private static final double MOOD_WEIGHT = 0.70;
    private static final double THEME_WEIGHT = 0.30;

    /** 추천 곡 수 */
    public static final int RESULT_SIZE = 5;

    /**
     * 동일 아티스트 최대 허용 곡 수. 현재는 RESULT_SIZE 와 동일해 사실상 비활성(가중치 2항 정책).
     * 음원 추가 수급 후 아티스트 편중이 보이면 이 값만 2로 낮추면 즉시 적용된다.
     */
    private static final int MAX_PER_ARTIST = RESULT_SIZE;

    private final MusicCacheService musicCache;

    /** 사진 분석 결과 기반 상위 5곡 선별. */
    public List<Scored> recommendByPhoto(PhotoAnalysisResult analysis) {
        double[] photoVector = analysis.moodVector();
        Set<PlaylistTheme> photoThemes = analysis.themeSet();

        List<Scored> ranked = musicCache.findAll().stream()
                .map(music -> new Scored(music, finalScore(photoVector, photoThemes, music)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(s -> s.music().getId()))
                .toList();

        return pickWithArtistCap(ranked);
    }

    /** 장르 기반 5곡 선별. 매 요청마다 셔플하여 동일 장르 재요청 시 구성이 달라지게 한다. */
    public List<Scored> recommendByGenre(Genre genre) {
        List<Music> candidates = new ArrayList<>(musicCache.findByGenre(genre));
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.GENRE_NOT_AVAILABLE);
        }

        java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
        return candidates.stream()
                .limit(RESULT_SIZE)
                .map(m -> new Scored(m, null))
                .toList();
    }

    private double finalScore(double[] photoVector, Set<PlaylistTheme> photoThemes, Music music) {
        return moodScore(photoVector, music.moodVector()) * MOOD_WEIGHT
                + themeScore(photoThemes, music.getThemes()) * THEME_WEIGHT;
    }

    /** moodScore = 1 - (d / sqrt(6)) */
    double moodScore(double[] photoVector, double[] musicVector) {
        double sumOfSquares = 0.0;
        for (int i = 0; i < Music.MOOD_DIMENSION; i++) {
            double diff = photoVector[i] - musicVector[i];
            sumOfSquares += diff * diff;
        }
        double distance = Math.sqrt(sumOfSquares);
        return 1.0 - (distance / MAX_DISTANCE);
    }

    /** themeScore = 교집합 수 / 사진 테마 수 */
    double themeScore(Set<PlaylistTheme> photoThemes, Set<PlaylistTheme> musicThemes) {
        if (photoThemes.isEmpty() || musicThemes.isEmpty()) {
            return 0.0;
        }
        long matched = photoThemes.stream().filter(musicThemes::contains).count();
        return (double) matched / photoThemes.size();
    }

    private List<Scored> pickWithArtistCap(List<Scored> ranked) {
        List<Scored> selected = new ArrayList<>(RESULT_SIZE);
        java.util.Map<String, Integer> artistCount = new java.util.HashMap<>();

        for (Scored candidate : ranked) {
            if (selected.size() == RESULT_SIZE) {
                break;
            }
            String artist = candidate.music().getArtist();
            int count = artistCount.getOrDefault(artist, 0);
            if (count >= MAX_PER_ARTIST) {
                continue;
            }
            selected.add(candidate);
            artistCount.put(artist, count + 1);
        }
        return selected;
    }

    /** 곡 + 최종 점수. 장르 추천은 점수가 없어 null. */
    public record Scored(Music music, Double score) {
    }
}
````

## 33. `src/main/java/com/soundscape/service/OpenAiVisionService.java`
> Jackson 3(tools.jackson) 사용. JsonNode 순회 대신 record 역직렬화로 API 리네임 영향을 차단했다.

````java
package com.soundscape.service;

import com.soundscape.dto.PhotoAnalysisResult;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPT-4o Vision 사진 감성 분석.
 *
 * 응답 지연 단축 3종 적용:
 *  1) 서버에서 이미지 최대 변 512px 리사이즈 후 JPEG 재인코딩 -> 업로드 페이로드 대폭 축소
 *  2) image_url.detail = low  -> 타일 분할 없이 단일 저해상도 패스로 처리
 *  3) response_format = json_schema (strict) -> 파싱 실패/재시도 제거, 필드 누락 없음
 *
 * [Spring Boot 4 / Jackson 3 주의]
 *  - databind 패키지가 com.fasterxml.jackson -> tools.jackson 으로 변경되었다.
 *  - JsonNode 순회 대신 타입 안전한 record 역직렬화를 사용해 Jackson 3 의 메서드 리네임 영향을 받지 않게 했다.
 *  - 애노테이션(@JsonIgnoreProperties 등)은 여전히 com.fasterxml.jackson.annotation 이다.
 */
@Slf4j
@Service
public class OpenAiVisionService {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final String SYSTEM_PROMPT = """
            너는 사진의 시각적 분위기를 정량화하는 분석 엔진이다.
            주어진 사진을 보고 6개 감정 수치를 0.0 이상 1.0 이하의 소수로 산출하고, 어울리는 테마를 1개 이상 3개 이하로 고른다.
            수치 정의:
            brightness = 화면의 밝기와 조도. 어두운 밤/실내면 낮고 햇살 가득한 낮이면 높다.
            energy = 활동성과 역동성. 정적인 풍경은 낮고 사람이 붐비거나 움직임이 큰 장면은 높다.
            calmness = 차분함과 고요함. 소란스러운 장면일수록 낮다.
            warmth = 색온도와 포근함. 주황/노랑 계열 따뜻한 톤이면 높고 푸른 한색 계열이면 낮다.
            positivity = 밝고 긍정적인 정서. 우울하거나 쓸쓸하면 낮다.
            dreaminess = 몽환적이고 아련한 느낌. 안개, 야경, 흐릿한 빛 번짐이 있으면 높다.
            반드시 지정된 JSON 스키마 형식으로만 응답한다.
            """;

    private final RestClient openAiRestClient;
    private final String model;
    private final String imageDetail;
    private final int imageMaxSize;

    public OpenAiVisionService(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            @Value("${openai.model}") String model,
            @Value("${openai.image-detail}") String imageDetail,
            @Value("${openai.image-max-size}") int imageMaxSize) {
        this.openAiRestClient = openAiRestClient;
        this.model = model;
        this.imageDetail = imageDetail;
        this.imageMaxSize = imageMaxSize;
    }

    public PhotoAnalysisResult analyze(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }

        String base64 = Base64.getEncoder().encodeToString(resizeToJpeg(photo));
        String requestBody = buildRequestBody(base64);

        try {
            // Boot 4 는 JSpecify 로 body() 를 nullable 로 명시한다. 반드시 null 검사할 것.
            String raw = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
            }

            String content = extractContent(MAPPER.readValue(raw, ChatResponse.class));
            return MAPPER.readValue(content, PhotoAnalysisResult.class).clamped();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Vision] 분석 실패", e);
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED, e);
        }
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
        }
        Choice choice = response.choices().get(0);
        if (choice == null || choice.message() == null
                || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
        }
        return choice.message().content();
    }

    /** 최대 변 imageMaxSize(기본 512px)로 축소 후 JPEG 바이트 반환. */
    private byte[] resizeToJpeg(MultipartFile photo) {
        try {
            BufferedImage source = ImageIO.read(photo.getInputStream());
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE);
            }

            int longestSide = Math.max(source.getWidth(), source.getHeight());
            double scale = longestSide > imageMaxSize ? (double) imageMaxSize / longestSide : 1.0;
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return out.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, e);
        }
    }

    private String buildRequestBody(String base64Jpeg) {
        String themeEnumJson = Arrays.stream(PlaylistTheme.values())
                .map(t -> "\"" + t.name() + "\"")
                .collect(Collectors.joining(","));

        String template = """
                {
                  "model": "%s",
                  "temperature": 0.2,
                  "max_tokens": 300,
                  "messages": [
                    { "role": "system", "content": %s },
                    { "role": "user", "content": [
                        { "type": "text", "text": "이 사진의 6대 감정 수치와 어울리는 테마를 산출하라." },
                        { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,%s", "detail": "%s" } }
                      ]
                    }
                  ],
                  "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                      "name": "photo_mood_analysis",
                      "strict": true,
                      "schema": {
                        "type": "object",
                        "properties": {
                          "brightness": { "type": "number" },
                          "energy":     { "type": "number" },
                          "calmness":   { "type": "number" },
                          "warmth":     { "type": "number" },
                          "positivity": { "type": "number" },
                          "dreaminess": { "type": "number" },
                          "themes": {
                            "type": "array",
                            "items": { "type": "string", "enum": [%s] }
                          }
                        },
                        "required": ["brightness","energy","calmness","warmth","positivity","dreaminess","themes"],
                        "additionalProperties": false
                      }
                    }
                  }
                }
                """;

        return String.format(template,
                model,
                MAPPER.writeValueAsString(SYSTEM_PROMPT),
                base64Jpeg,
                imageDetail,
                themeEnumJson);
    }

    // ---------- OpenAI 응답 매핑 (JsonNode 순회 대신 타입 안전 역직렬화) ----------

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    record Message(String content) {
    }
}
````

## 34. `src/main/java/com/soundscape/service/S3Service.java`
> 커버 업로드 및 Presigned URL 발급.

````java
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

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot).toLowerCase() : "";
    }
}
````

## 35. `src/main/java/com/soundscape/service/PlaylistService.java`
> 유스케이스 조립. requireUser()가 X-User-Id 검증을 담당.

````java
package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.domain.Playlist;
import com.soundscape.domain.PlaylistMusic;
import com.soundscape.domain.User;
import com.soundscape.dto.*;
import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.PlaylistRepository;
import com.soundscape.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final MusicCacheService musicCache;
    private final MusicRecommendationService recommendationService;
    private final OpenAiVisionService visionService;
    private final S3Service s3Service;

    /** 사진 기반 추천. 저장은 하지 않고 5곡 Presigned URL 만 반환한다. */
    public RecommendationResponse recommendByPhoto(MultipartFile photo) {
        PhotoAnalysisResult analysis = visionService.analyze(photo);
        List<MusicResponse> musics = toMusicResponses(recommendationService.recommendByPhoto(analysis));
        return RecommendationResponse.photo(analysis, musics);
    }

    /** 장르 기반 추천. 해당 장르 음원이 0곡이면 GENRE_NOT_AVAILABLE(404). */
    public RecommendationResponse recommendByGenre(Genre genre) {
        List<MusicResponse> musics = toMusicResponses(recommendationService.recommendByGenre(genre));
        return RecommendationResponse.genre(genre, musics);
    }

    /** 플리 저장: 원본 사진 S3 업로드 + 플리 + 곡 position 저장. */
    @Transactional
    public PlaylistDetailResponse save(Long userId, PlaylistSaveRequest request, MultipartFile photo) {
        User user = requireUser(userId);

        if (!musicCache.existsAll(request.getMusicIds())) {
            throw new BusinessException(ErrorCode.INVALID_MUSIC_IDS);
        }

        String coverObjectKey = (photo != null && !photo.isEmpty()) ? s3Service.uploadCover(photo) : null;
        Genre sourceGenre = request.getSourceType() == SourceType.GENRE ? request.getSourceGenre() : null;

        Playlist playlist = Playlist.create(
                user,
                request.getTitle(),
                request.getSourceType(),
                sourceGenre,
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

    private PlaylistDetailResponse toDetail(Playlist playlist) {
        List<MusicResponse> musics = playlist.getItems().stream()
                .sorted(java.util.Comparator.comparingInt(PlaylistMusic::getPosition))
                .map(item -> musicCache.findById(item.getMusicId()))
                .filter(java.util.Objects::nonNull)
                .map(music -> MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), null))
                .toList();

        return PlaylistDetailResponse.of(playlist, s3Service.presignedUrl(playlist.getCoverObjectKey()), musics);
    }

    private List<MusicResponse> toMusicResponses(List<MusicRecommendationService.Scored> scoredList) {
        return scoredList.stream()
                .map(scored -> {
                    Music music = scored.music();
                    Double score = scored.score() == null
                            ? null
                            : Math.round(scored.score() * 10000) / 10000.0;
                    return MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), score);
                })
                .toList();
    }

    /** 임시 인증: X-User-Id 헤더 검증. 미전송 401, 미존재 사용자 401. */
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
````

# controller

## 36. `src/main/java/com/soundscape/controller/PlaylistController.java`
> 5개 핵심 API.

````java
package com.soundscape.controller;

import com.soundscape.dto.*;
import com.soundscape.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlaylistController {

    private static final String USER_HEADER = "X-User-Id";

    private final PlaylistService playlistService;

    /** 1. 사진 기반 추천 (인증 불필요) */
    @PostMapping(value = "/playlists/recommendations/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecommendationResponse recommendByPhoto(@RequestPart("photo") MultipartFile photo) {
        return playlistService.recommendByPhoto(photo);
    }

    /** 2. 장르 기반 추천 (인증 불필요) */
    @PostMapping(value = "/playlists/recommendations/genre", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RecommendationResponse recommendByGenre(@Valid @RequestBody GenreRecommendationRequest request) {
        return playlistService.recommendByGenre(request.genre());
    }

    /** 3. 플리 저장 (X-User-Id 필수) */
    @PostMapping(value = "/playlists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistDetailResponse save(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @Valid @ModelAttribute PlaylistSaveRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return playlistService.save(userId, request, photo);
    }

    /** 4. 플리 목록 조회 (X-User-Id 필수) */
    @GetMapping("/playlists")
    public List<PlaylistSummaryResponse> findMyPlaylists(
            @RequestHeader(value = USER_HEADER, required = false) Long userId) {
        return playlistService.findMyPlaylists(userId);
    }

    /** 5. 플리 상세 조회 (X-User-Id 필수) */
    @GetMapping("/playlists/{playlistId}")
    public PlaylistDetailResponse findDetail(
            @RequestHeader(value = USER_HEADER, required = false) Long userId,
            @PathVariable Long playlistId) {
        return playlistService.findDetail(userId, playlistId);
    }
}
````

## 37. `src/main/java/com/soundscape/controller/AdminController.java`
> 음원 추가 수급 후 캐시 재적재용 운영 엔드포인트.

````java
package com.soundscape.controller;

import com.soundscape.service.MusicCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 음원 추가 수급 후 서버 재시작 없이 인메모리 캐시를 갱신하기 위한 운영용 엔드포인트. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MusicCacheService musicCacheService;

    @PostMapping("/music/reload")
    public Map<String, Object> reloadMusicCache() {
        int size = musicCacheService.reload();
        return Map.of("reloaded", true, "musicCount", size);
    }
}
````

# test

## 38. `src/test/java/com/soundscape/service/MusicRecommendationServiceTest.java`
> DB/외부 API 없이 수식만 검증. gradlew test로 단독 실행 가능.

````java
package com.soundscape.service;

import com.soundscape.enums.PlaylistTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 정규화 수식 검증. 캐시 의존 없이 순수 계산만 테스트한다. */
class MusicRecommendationServiceTest {

    private final MusicRecommendationService service = new MusicRecommendationService(null);

    @Test
    @DisplayName("완전 일치 시 moodScore = 1.0")
    void moodScore_identical() {
        double[] v = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
        assertEquals(1.0, service.moodScore(v, v), 1e-9);
    }

    @Test
    @DisplayName("최대 거리(모든 축 0 vs 1) 시 moodScore = 0.0")
    void moodScore_maxDistance() {
        double[] zero = {0, 0, 0, 0, 0, 0};
        double[] one = {1, 1, 1, 1, 1, 1};
        assertEquals(0.0, service.moodScore(zero, one), 1e-9);
    }

    @Test
    @DisplayName("moodScore는 항상 0.0 ~ 1.0 범위")
    void moodScore_range() {
        double[] a = {0.1, 0.9, 0.2, 0.8, 0.3, 0.7};
        double[] b = {0.9, 0.1, 0.8, 0.2, 0.7, 0.3};
        double score = service.moodScore(a, b);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    @DisplayName("themeScore = 교집합 수 / 사진 테마 수")
    void themeScore_ratio() {
        Set<PlaylistTheme> photo = Set.of(PlaylistTheme.RAINY_DAY, PlaylistTheme.REST_HEALING);
        Set<PlaylistTheme> music = Set.of(PlaylistTheme.RAINY_DAY, PlaylistTheme.STUDY_FOCUS);
        assertEquals(0.5, service.themeScore(photo, music), 1e-9);
    }

    @Test
    @DisplayName("테마가 비어 있으면 themeScore = 0")
    void themeScore_empty() {
        assertEquals(0.0, service.themeScore(Set.of(), Set.of(PlaylistTheme.RAINY_DAY)), 1e-9);
    }
}
````

# 문서

## 39. `README.md`
> 실행 순서, API curl 예시, Spring Boot 4 주의사항.

````markdown
# SoundScape Backend

Spring Boot 4.0.7 / Spring Framework 7 / Jackson 3 / Java 21 / MySQL 8 / AWS S3 / GPT-4o Vision

---

## 1. 실행 순서

```bash
# 1) DB 생성 + 시드 (MySQL Workbench 또는 CLI)
mysql -u root -p < src/main/resources/db/schema.sql
mysql -u root -p < src/main/resources/db/seed.sql

# 2) 환경변수
export DB_USERNAME=root
export DB_PASSWORD=****
export AWS_ACCESS_KEY=****
export AWS_SECRET_KEY=****
export OPENAI_API_KEY=sk-****

# 3) 기동
./gradlew bootRun
```

기동 로그에 `[MusicCache] 음원 26곡 적재 완료. 장르별 보유: ...` 이 찍히면 정상이다.

---

## 2. S3 버킷 준비

- 버킷명: `soundscape-bucket-sangjin` / 리전: `ap-northeast-2`
- 퍼블릭 액세스 차단 유지 (Presigned URL 로만 접근)
- 업로드 경로는 `music` 테이블의 `audio_object_key` 와 정확히 일치해야 한다.
  - 예: `music/ask.mp3`, `music/smooth.mp3` ... (총 26개)
- 커버 이미지는 서버가 `covers/{uuid}.jpg` 로 자동 업로드한다.

---

## 3. API

Base URL: `http://localhost:8080/api/v1`
공통 에러 응답: `{ "timestamp", "status", "code", "message" }`

| 기능 | Method | Endpoint | Header |
| --- | --- | --- | --- |
| 사진 기반 추천 | POST | `/playlists/recommendations/photo` | 불필요 |
| 장르 기반 추천 | POST | `/playlists/recommendations/genre` | 불필요 |
| 플리 저장 | POST | `/playlists` | `X-User-Id` |
| 플리 목록 | GET | `/playlists` | `X-User-Id` |
| 플리 상세 | GET | `/playlists/{playlistId}` | `X-User-Id` |
| 음원 캐시 갱신 | POST | `/admin/music/reload` | 불필요 |

### curl 예시

```bash
# 사진 기반 추천
curl -X POST http://localhost:8080/api/v1/playlists/recommendations/photo \
  -F "photo=@/path/to/photo.jpg"

# 장르 기반 추천
curl -X POST http://localhost:8080/api/v1/playlists/recommendations/genre \
  -H "Content-Type: application/json" \
  -d '{"genre":"POP"}'

# 플리 저장 (musicIds 는 반복 전송)
curl -X POST http://localhost:8080/api/v1/playlists \
  -H "X-User-Id: 1" \
  -F "photo=@/path/to/photo.jpg" \
  -F "title=비 오는 날의 카페" \
  -F "sourceType=PHOTO" \
  -F "musicIds=3" -F "musicIds=10" -F "musicIds=14" -F "musicIds=4" -F "musicIds=1"

# 목록 / 상세
curl -H "X-User-Id: 1" http://localhost:8080/api/v1/playlists
curl -H "X-User-Id: 1" http://localhost:8080/api/v1/playlists/1
```

### 프론트 FormData 주의사항

플리 저장은 JSON part 가 아닌 **일반 폼 필드**로 받는다. Blob content-type 설정이 필요 없다.

```js
const fd = new FormData();
fd.append('photo', file);
fd.append('title', title);
fd.append('sourceType', 'PHOTO');          // 또는 'GENRE'
// fd.append('sourceGenre', 'POP');        // sourceType=GENRE 일 때만
musicIds.forEach(id => fd.append('musicIds', id));
fetch(url, { method: 'POST', headers: { 'X-User-Id': '1' }, body: fd });
```

---

## 4. 추천 알고리즘

```
Final Score = (moodScore × 0.70) + (themeScore × 0.30)

moodScore  = 1 - ( d / √6 )        // d = 6차원 유클리드 거리, √6 ≈ 2.4495 = 최대 거리
themeScore = |사진테마 ∩ 곡테마| / |사진테마|
```

- 두 점수 모두 0.0 ~ 1.0 으로 정규화되어 가중합이 그대로 유효하다.
- `Music.moodVector()` 와 `PhotoAnalysisResult.moodVector()` 의 **인덱스 순서는 반드시 동일**해야 한다
  (brightness, energy, calmness, warmth, positivity, dreaminess).
- `MusicRecommendationService.MAX_PER_ARTIST` 는 현재 5(비활성). 음원 추가 수급 후 아티스트 편중이 보이면
  이 상수만 2로 낮추면 즉시 적용된다.

---

## 5. 장르 확장

`Genre` enum 7개는 그대로 유지한다. 현재 DB 보유 곡은 3개 장르뿐이므로
`HIPHOP_RAP` / `RNB_SOUL` / `KPOP` / `JPOP_ANIME_OST` 요청 시
`404 GENRE_NOT_AVAILABLE` 이 반환된다. 프론트는 이 코드를 받으면 "준비 중" 안내를 띄우면 된다.

음원 추가 시 절차:
1. S3 에 MP3 업로드
2. `music` / `music_theme` INSERT
3. `POST /api/v1/admin/music/reload` 호출 (서버 재시작 불필요)

---

## 6. 성능 설계

| 항목 | 처리 |
| --- | --- |
| 음원 조회 | `@PostConstruct` 로 전량 인메모리 적재. 추천 시 DB 접근 0회 |
| music_theme | `@ElementCollection` 매핑. 복합키 Entity/Repository 불필요 |
| 초기 로딩 | `findAllWithThemes()` fetch join 으로 N+1 제거 |
| Vision 지연 | 512px 리사이즈 + `detail: low` + `json_schema` strict |
| HTTP 타임아웃 | `spring.http.clients.*` (Boot 4 신규 네임스페이스) |
| 플리 상세 | `music_id` 만 저장하고 캐시에서 해석 → music 테이블 조인 없음 |


---

## 7. Spring Boot 4 관련 주의사항

| 항목 | 내용 |
| --- | --- |
| Java | Boot 4.0.x 는 Java 21 이상 권장. Initializr 로 받은 값을 그대로 사용해도 된다 |
| 스타터 이름 | `spring-boot-starter-web` → `spring-boot-starter-webmvc` (구 이름도 동작하나 deprecated) |
| Jackson | Jackson 3 기본 탑재. `com.fasterxml.jackson.databind` → `tools.jackson.databind`. **단 애노테이션은 `com.fasterxml.jackson.annotation` 유지** |
| HTTP 타임아웃 | `spring.http.client.*` → `spring.http.clients.*` 로 변경됨 |
| RestClient | `body()` 가 JSpecify nullable 로 명시됨. null 검사 필수 |
| Hibernate | Hibernate 7. dialect 명시 불필요 (자동 감지) |
| 테스트 | JUnit 4 전이 의존성 제거됨. `junit-platform-launcher` 를 testRuntimeOnly 로 선언 |

`spring-boot-properties-migrator` 를 임시로 넣으면 기동 시 deprecated 프로퍼티를 로그로 알려준다. 마이그레이션이 끝나면 제거할 것.
````

---

# 부록. 복원 후 검증 순서

> **[2026-08-10 진행 상태 갱신]**

1. ✅ `.\gradlew compileJava` — BUILD SUCCESSFUL, 의존성/Jackson import 오류 없음 확인 완료
2. ✅ `schema.sql` → `seed.sql` 순서 실행 완료 — `bootRun` 로그에서 `[MusicCache] 음원 26곡 적재 완료. 장르별 보유: LOFI_INSTRUMENTAL=8, JAZZ_CLASSICAL_NEWAGE=10, POP=8` 확인으로 간접 검증됨 (COUNT 직접 조회는 미실시)
3. ❌ `.\gradlew test` — **미해결**. `ClassNotFoundException` 발생 (원인: 프로젝트 경로의 한글 폴더명 + Gradle 인자 파일 인코딩 문제. 런타임 트러블슈팅 표 참조)
4. ⚠️ `.\gradlew bootRun` — MusicCache 적재까지는 성공하나, `AwsS3Config` 에서 AWS 자격증명 미설정으로 `S3Client` 빈 생성 실패 → 애플리케이션 최종 기동 미완료. `AWS_ACCESS_KEY`/`AWS_SECRET_KEY` 값(실키 또는 더미) 설정 후 재시도 필요
5. ⬜ `POST /api/v1/playlists/recommendations/genre` 검증 — 미실시 (4번 선행 필요)
6. ⬜ `{"genre":"KPOP"}` → `404 GENRE_NOT_AVAILABLE` 검증 — 미실시
7. ⬜ S3 버킷 생성 및 MP3 26개 업로드 후 사진 추천 API 검증 — 미실시

## 컴파일 실패 시 우선 확인

| 에러 | 조치 |
| :--- | :--- |
| `package com.fasterxml.jackson.databind does not exist` | `tools.jackson.databind` 로 변경. 애노테이션은 그대로 |
| `cannot find symbol: SimpleClientHttpRequestFactory` | `OpenAiConfig` 를 오토컨피그 `RestClient.Builder` 주입 방식으로 교체 |
| `package jakarta.validation does not exist` | `spring-boot-starter-validation` 누락 |
| `cannot find symbol: S3Client` | `software.amazon.awssdk:bom` + `s3` 누락 |
| 오토컨피그 미동작 | 모듈화로 스타터 분리됨. 임시로 `spring-boot-starter-classic` 추가 후 원인 좁히기 |

## 🆕 런타임 트러블슈팅 (2026-08-10 복원 작업 중 실제로 겪은 항목)

| 증상 | 원인 / 조치 |
| :--- | :--- |
| `bootRun` 시 `Access denied for user 'root'@'localhost'` (DB 계정을 바꿨는데도 재현) | 프로젝트 **루트**에 예전 버전 `application.properties` 잔여 파일이 있으면 Spring Boot 의 기본 설정 위치(`file:./application.properties`)가 `classpath:/application.properties`(=`src/main/resources`)보다 **우선 적용**되어 새 설정이 무시된다. 루트의 잔여 `application.properties` 를 찾아 삭제할 것. (`OpenAiConfig.java`, `OpenAiVisionService.java` 도 같은 방식으로 루트에 잘못 떨어져 있을 수 있으니 함께 확인) |
| `No qualifying bean of type 'org.springframework.web.client.RestClient$Builder'` | Spring Boot 4 는 `RestClientAutoConfiguration` 을 `spring-boot-restclient` 모듈로 분리했고, `spring-boot-starter-webmvc` 는 이를 가져오지 않는다. `build.gradle` 에 `implementation 'org.springframework.boot:spring-boot-starter-restclient'` 를 추가해야 `OpenAiConfig` 의 `RestClient.Builder` 주입이 성공한다 |
| `NullPointerException: Access key ID cannot be blank` (`AwsS3Config.s3Client()`) | `aws.credentials.access-key` / `secret-key` 가 빈 문자열로 주입됨. `AwsS3Config` 가 기동 시점에 `S3Client` 빈을 즉시(eager) 생성하며 `AwsBasicCredentials.create()` 를 호출하는데, blank 값은 검증에서 즉시 예외가 난다. `AWS_ACCESS_KEY`/`AWS_SECRET_KEY` 환경변수를 설정하거나, 로컬 기동만 확인할 목적이면 `application.properties` 기본값을 `${AWS_ACCESS_KEY:dummy}` 처럼 더미 문자열로 채워 빈 값 자체를 방지한다(실제 S3 호출 시엔 여전히 인증 실패) |
| MySQL `root` 계정으로 `Access denied` | `root` 비밀번호를 모르거나 원격 접속이 막혀 있는 환경에서는 붙지 말고, 전용 계정을 만들어 우회한다: `CREATE USER 'soundscape'@'localhost' IDENTIFIED BY '비밀번호'; GRANT ALL PRIVILEGES ON soundscape.* TO 'soundscape'@'localhost'; FLUSH PRIVILEGES;` 후 `application.properties` 의 `spring.datasource.username`/`password` 를 이 계정으로 교체 |
| `.\gradlew test` 실행 시 `Could not execute test class ... ClassNotFoundException` (테스트 클래스는 `build/classes/java/test` 에 정상 컴파일되어 있음) | 프로젝트 경로에 **한글(비 ASCII) 사용자 폴더명**이 포함된 경우(예: `C:\Users\김상진\...`) 발생. Gradle 은 긴 classpath 를 `@gradle-worker-classpath....txt` 인자 파일로 넘기는데, 이 파일은 UTF-8(BOM 없음)로 작성되지만 `java.exe` 는 BOM 이 없으면 **OS 기본 코드페이지**(한국어 Windows는 CP949)로 그 파일을 해석한다. 그 결과 한글이 포함된 `build\classes\java\test` 경로만 깨져서 로드되지 못하고, ASCII 경로인 `GRADLE_USER_HOME`(`C:\.gradle\caches\...`) 아래 의존성 jar 들은 정상 로드된다. **근본 해결**: 프로젝트를 ASCII 경로(예: `C:\dev\soundscape`)로 옮기거나, Windows 설정에서 "세계 언어 지원을 위한 유니코드 UTF-8 사용"을 활성화(재부팅 필요) — 둘 다 코드/문서 수정만으로는 해결 불가한 OS/환경 이슈이므로 별도 조치 필요 |
