# SoundScape Backend

사용자가 촬영한 사진 한 장을 GPT-4o Vision으로 분석해, 그 공간의 장소·활동·날씨·시간대·분위기에 어울리는 음악 5곡을 추천하고 플레이리스트로 저장하는 서비스의 백엔드입니다.

**기술 스택**: Spring Boot 4.0.7 / Spring Framework 7 / Jackson 3 / Java 17 / MySQL 8 / AWS S3 / GPT-4o Vision

---

## 1. 실행 방법

### 로컬 개발

```bash
# 1) DB 생성 + 시드
mysql -u root -p < src/main/resources/db/schema.sql
mysql -u root -p < src/main/resources/db/seed.sql

# 2) 환경변수 (미설정 시 기본값은 application.properties 참고)
export DB_USERNAME=soundscape
export DB_PASSWORD=****
export AWS_ACCESS_KEY=****
export AWS_SECRET_KEY=****
export OPENAI_API_KEY=sk-****

# 3) 기동
./gradlew bootRun
```

기동 로그에 `[MusicCache] 음원 N곡 적재 완료. 장르별 보유: ...` 가 찍히면 정상입니다.

### 배포 (Docker)

`.env.example` 을 `.env` 로 복사해서 값을 채운 뒤:

```bash
docker build -t soundscape .
docker run --env-file .env -p 8080:8080 soundscape
```

`.env` 필수 항목: `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `OPENAI_API_KEY`, `CORS_ALLOWED_ORIGINS`(프론트 도메인), `ADMIN_API_KEY`. `prod` 프로파일은 이 값들에 기본값이 없어서 누락 시 기동이 실패합니다(의도된 fail-closed 동작).

---

## 2. S3 버킷 준비

- 버킷명: `soundscape-bucket-sangjin` / 리전: `ap-northeast-2`
- 퍼블릭 액세스 차단 유지 (Presigned URL 로만 접근)
- 업로드 경로는 `music` 테이블의 `audio_object_key` 와 정확히 일치해야 합니다.
- 커버 이미지는 서버가 플레이리스트 저장 시점에 `covers/{uuid}.jpg` 로 자동 업로드합니다(추천 단계에서는 업로드하지 않음).

---

## 3. API

Base URL: `http://localhost:8080/api/v1`
공통 에러 응답: `{ "timestamp", "status", "code", "message" }`

현재 MVP는 **사진 기반 추천만 지원**합니다(장르 기반 추천은 스코프 아웃).

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 사진 기반 추천 | POST | `/playlists/recommendations/photo` | 불필요 (IP당 분당 10회 제한) |
| 플레이리스트 저장 | POST | `/playlists` | `X-User-Id` |
| 플레이리스트 목록 | GET | `/playlists` | `X-User-Id` |
| 플레이리스트 상세 | GET | `/playlists/{playlistId}` | `X-User-Id` |
| 음원 캐시 갱신(운영용) | POST | `/admin/music/reload` | `X-Admin-Key` |

### 3.1 사진 기반 추천

```
POST /playlists/recommendations/photo
Content-Type: multipart/form-data

image: (File, 필수)
```

```json
{
  "title": "비 오는 밤의 카페",
  "sourceType": "PHOTO_RECOMMENDATION",
  "sourceGenre": null,
  "analysis": {
    "place": "CAFE_LIBRARY",
    "activity": "STUDY_WORK",
    "weather": "RAINY",
    "time": "NIGHT",
    "mood": {
      "brightness": 0.25, "energy": 0.30, "calmness": 0.85,
      "warmth": 0.75, "positivity": 0.45, "dreaminess": 0.80
    }
  },
  "convertedThemes": ["STUDY_FOCUS", "RAINY_DAY", "NIGHT_DREAMY"],
  "musicList": [
    {
      "musicId": 8, "title": "Rainy Night Study", "artist": "Pixabay Creator",
      "genre": "LOFI_INSTRUMENTAL", "durationSeconds": 183,
      "themes": ["STUDY_FOCUS", "RAINY_DAY"],
      "audioUrl": "https://s3-presigned-url...",
      "score": 0.9234, "position": 1
    }
  ]
}
```

`title`/`analysis`는 AI가 사진 한 장으로 산출하고, 그 결과는 DB에 저장되지 않습니다. `musicList`는 항상 5곡입니다.

### 3.2 플레이리스트 저장

```
POST /playlists
X-User-Id: 1
Content-Type: multipart/form-data

request: (JSON Blob, 필수) { "title": "...", "sourceType": "PHOTO_RECOMMENDATION", "musicIds": [8,21,4,13,9] }
image:   (File, 필수 — 추천에 썼던 원본 사진)
```

응답 201:

```json
{
  "playlistId": 10,
  "title": "비 오는 밤의 카페",
  "sourceType": "PHOTO_RECOMMENDATION",
  "sourceGenre": null,
  "coverImageUrl": "https://s3-presigned-url...",
  "createdAt": "2026-08-16T16:40:00",
  "musicList": [ /* 3.1 과 동일한 형태, score 는 null */ ]
}
```

검증 규칙: `musicIds`는 정확히 5개, 중복 불가, 전부 존재하는 음악 ID, 사진 필수. 요청 본문에 `userId`는 넣지 않습니다(헤더로만 식별).

### 3.3 플레이리스트 목록 / 상세

```
GET /playlists          X-User-Id: 1
GET /playlists/{id}     X-User-Id: 1
```

목록 응답:

```json
{
  "playlists": [
    {
      "playlistId": 10, "title": "비 오는 밤의 카페",
      "sourceType": "PHOTO_RECOMMENDATION", "sourceGenre": null,
      "coverImageUrl": "https://s3-presigned-url...",
      "createdAt": "2026-08-16T16:40:00"
    }
  ]
}
```

상세 응답은 3.2 저장 응답과 동일한 구조입니다. 다른 사용자의 플레이리스트이거나 존재하지 않으면 동일하게 `404 PLAYLIST_NOT_FOUND`를 반환합니다(존재 여부를 유추할 수 없도록).

### 3.4 관리자: 음원 캐시 갱신

```
POST /admin/music/reload
X-Admin-Key: {ADMIN_API_KEY 환경변수와 동일한 값}
```

음원을 S3/DB에 추가한 뒤 서버 재시작 없이 인메모리 캐시를 다시 적재합니다. `ADMIN_API_KEY`가 비어있으면 항상 거부됩니다.

### 3.5 에러 코드

| 코드 | 상태 | 설명 |
| --- | --- | --- |
| `USER_ID_REQUIRED` | 401 | `X-User-Id` 헤더 없음 |
| `INVALID_USER` | 401 | 존재하지 않는 사용자 |
| `ADMIN_UNAUTHORIZED` | 401 | `X-Admin-Key` 불일치/누락 |
| `TOO_MANY_REQUESTS` | 429 | 사진 추천 rate limit 초과 (IP당 분당 10회) |
| `INVALID_IMAGE` | 400 | 이미지 파일을 읽을 수 없음 |
| `EMPTY_FILE` | 400 | 파일이 비어 있음 |
| `FILE_TOO_LARGE` | 413 | 20MB 초과 |
| `INVALID_REQUEST` | 400 | 요청 값 형식 오류(필수 파트 누락, enum 값 오류 등) |
| `INVALID_MUSIC_COUNT` | 400 | `musicIds`가 5개가 아님 |
| `DUPLICATE_MUSIC` | 400 | `musicIds` 중복 |
| `IMAGE_REQUIRED` | 400 | 저장 요청에 사진 누락 |
| `MUSIC_NOT_FOUND` | 404 | 존재하지 않는 음악 ID 포함 |
| `PLAYLIST_NOT_FOUND` | 404 | 플레이리스트 없음 또는 타인 소유 |
| `VISION_ANALYSIS_FAILED` | 502 | OpenAI 분석 실패 |
| `S3_UPLOAD_FAILED` | 502 | S3 업로드 실패 |
| `INTERNAL_ERROR` | 500 | 그 외 서버 오류 |

### 3.6 Enum 값

```
Genre(음악 주장르, 곡마다 1개):
  LOFI_INSTRUMENTAL, JAZZ_CLASSICAL_NEWAGE, POP, HIPHOP_RAP, RNB_SOUL, KPOP, JPOP_ANIME_OST

PlaylistTheme(음악 테마, 곡마다 여러 개 가능):
  STUDY_FOCUS, REST_HEALING, WALK_DRIVE_TRAVEL, RAINY_DAY, NIGHT_DREAMY, BRIGHT_DAY, PARTY_ENERGY

PlaceType:    CAFE_LIBRARY, ROOM_HOME, STREET_CITY, TRANSPORT, NATURE_TRAVEL, SOCIAL_SPACE, OTHER
ActivityType: STUDY_WORK, REST_HEALING, WALK_DRIVE_TRAVEL, EATING_CAFE, SOCIAL_PARTY, UNKNOWN
WeatherType:  SUNNY, CLOUDY, RAINY, SNOWY, UNKNOWN
TimeType:     DAY, SUNSET, NIGHT, UNKNOWN
```

---

## 4. 추천 알고리즘

### 4.1 사진 → 테마 변환 (`PhotoThemeScoringService`)

AI가 place/activity/weather/time/mood를 산출하면, 고정 규칙표로 테마별 점수를 매겨 0.5점 이상인 테마를 최대 3개(점수 높은 순) 고릅니다. 하나도 없으면 최고점 테마 1개, 전부 0점이면 `REST_HEALING`을 기본값으로 씁니다. 동점이면 `PlaylistTheme` enum 선언 순서로 우선순위를 정합니다.

| 분석값 | 변환 테마 | 점수 |
| --- | --- | --- |
| activity=STUDY_WORK | STUDY_FOCUS | +1.0 |
| activity=REST_HEALING | REST_HEALING | +1.0 |
| activity=WALK_DRIVE_TRAVEL | WALK_DRIVE_TRAVEL | +1.0 |
| activity=SOCIAL_PARTY | PARTY_ENERGY | +1.0 |
| weather=RAINY | RAINY_DAY | +0.8 |
| weather=SUNNY | BRIGHT_DAY | +0.5 |
| time=NIGHT | NIGHT_DREAMY | +0.7 |
| time=DAY | BRIGHT_DAY | +0.3 |
| place=CAFE_LIBRARY | STUDY_FOCUS | +0.2 |
| place=ROOM_HOME | REST_HEALING | +0.2 |
| place=TRANSPORT / NATURE_TRAVEL | WALK_DRIVE_TRAVEL | +0.3 |
| place=SOCIAL_SPACE | PARTY_ENERGY | +0.3 |
| calmness ≥ 0.7 | REST_HEALING | +0.2 |
| dreaminess ≥ 0.7 | NIGHT_DREAMY | +0.2 |
| energy ≥ 0.7 & positivity ≥ 0.6 | PARTY_ENERGY | +0.2 |
| brightness ≥ 0.7 & positivity ≥ 0.6 | BRIGHT_DAY | +0.2 |

### 4.2 곡 추천 점수 (`MusicRecommendationService`)

```
Final Score = (moodScore × 0.70) + (themeScore × 0.30)

moodScore  = 1 - (d / √6)        // d = 6차원 유클리드 거리, √6 ≈ 2.4495 = 최대 거리
themeScore = |사진테마 ∩ 곡테마| / |사진테마|
```

- 두 점수 모두 0.0 ~ 1.0으로 정규화되어 가중합이 그대로 유효합니다.
- `Music.moodVector()`와 `PhotoAnalysisResult.moodVector()`의 **인덱스 순서는 반드시 동일**해야 합니다(brightness, energy, calmness, warmth, positivity, dreaminess).
- `MAX_PER_ARTIST`(현재 5, 비활성)를 낮추면 동일 아티스트 편중을 제한할 수 있습니다.

---

## 5. 음원 확장

`Genre` enum 7개 중 현재 DB 보유 곡은 `LOFI_INSTRUMENTAL`/`JAZZ_CLASSICAL_NEWAGE`/`POP` 뿐입니다. 나머지 4개 장르는 음원 추가 수급 예정입니다. 사진 기반 추천은 장르와 무관하게 전체 곡을 대상으로 하므로, 장르 보유 현황이 추천 자체를 막지는 않습니다.

음원 추가 절차:
1. S3에 MP3 업로드
2. `music` / `music_theme` INSERT
3. `POST /admin/music/reload` 호출 (서버 재시작 불필요)

---

## 6. 성능 설계

| 항목 | 처리 |
| --- | --- |
| 음원 조회 | `@PostConstruct`로 전량 인메모리 적재. 추천 시 DB 접근 0회 |
| music_theme | `@ElementCollection` 매핑. 복합키 Entity/Repository 불필요 |
| 초기 로딩 | `findAllWithThemes()` fetch join으로 N+1 제거 |
| Vision 지연 | 512px 리사이즈 + `detail: low` + `json_schema` strict |
| 플리 상세 | `music_id`만 저장하고 캐시에서 해석 → music 테이블 조인 없음 |
| Rate limit | 메모리 기반 슬라이딩 윈도우(`RateLimiter`), 10분마다 오래된 IP 항목 정리 |

---

## 7. Spring Boot 4 관련 주의사항

| 항목 | 내용 |
| --- | --- |
| Java | Boot 4.0.x는 Java 21 이상을 권장하지만 필수는 아님. 이 프로젝트는 Java 17 툴체인으로 문제없이 동작 |
| 스타터 이름 | `spring-boot-starter-web` → `spring-boot-starter-webmvc` |
| Jackson | Jackson 3 기본 탑재. `com.fasterxml.jackson.databind` → `tools.jackson.databind`. **애노테이션은 `com.fasterxml.jackson.annotation` 유지** |
| HTTP 타임아웃 | `spring.http.client.*` → `spring.http.clients.*` |
| RestClient | `body()`가 JSpecify nullable로 명시됨. null 검사 필수 |
| Hibernate | Hibernate 7. dialect 명시 불필요 |
