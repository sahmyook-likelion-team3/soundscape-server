# 개발 중 발견한 문제와 해결 기록

이 문서는 백엔드 개발/검수 과정에서 실제로 발견된 문제와 그 해결 방법을 정리한 기록이다. "돌아가는 코드"에서 멈추지 않고, 코드 리뷰·명세서 대조·배포 준비 과정에서 드러난 결함을 어떤 근거로 얼마나 심각하다고 판단했고 어떻게 고쳤는지를 남긴다.

---

## 1. 보안·안정성 결함

MVP는 회원가입/로그인을 의도적으로 생략하고 `X-User-Id` 헤더로 사용자를 임시 식별한다. 이 설계 자체는 트레이드오프로 남겨뒀지만, 그 외에 **설계 의도와 무관하게 실제로 뚫려 있던 구멍들**은 별도로 찾아서 막았다.

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| 관리자 API(`POST /admin/music/reload`)에 인증이 전혀 없음 | 내부 운영용이라 인증을 안 넣고 넘어감 | `X-Admin-Key` 헤더 검증 추가. `ADMIN_API_KEY` 환경변수가 비어 있으면 무조건 거부(fail-closed)하도록 해서, 배포 시 설정을 깜빡해도 뚫리지 않게 함 |
| 사진 추천 API가 인증·횟수 제한 없이 유료 GPT-4o Vision을 호출 | 요청마다 실제 과금이 발생하는데 아무 제한이 없었음 | IP 기반 rate limiter 추가(분당 10회). 첫 구현은 `X-Forwarded-For` 헤더를 그대로 신뢰해서 공격자가 헤더값만 바꾸면 우회 가능했던 걸 재검토에서 발견 → `request.getRemoteAddr()`만 쓰도록 수정 |
| rate limiter의 IP별 요청 기록이 계속 쌓여 메모리 누수 | 오래된 IP 항목을 정리하는 로직이 없었음 | `@Scheduled`로 10분마다 최근 요청 없는 IP 항목을 제거하도록 추가 |
| 업로드 용량(15MB) 초과 시 500 에러로 응답 | `MaxUploadSizeExceededException`을 별도로 처리하지 않아 일반 오류 핸들러로 새어나감 | 전용 예외 핸들러 추가, 20MB로 상향 조정 후 413 + 명확한 안내 메시지 반환 |
| 잘못된 enum 값, 필수 파트 누락, 경로변수 타입 오류가 전부 500으로 응답 | `HttpMessageNotReadableException` 등 흔한 파싱 예외를 처리하는 핸들러가 없었음 | 3종 예외를 묶어 400(`INVALID_REQUEST`)으로 매핑하는 핸들러 추가 |
| 저장 요청에서 `musicIds` 개수/중복 제한이 없음 | 검증 로직 자체가 없었음 | 정확히 5개, 중복 불가, 전부 존재하는 ID인지 검증 추가 |
| `title`이 빈 문자열이거나 200자를 넘어도 저장돼서 DB 오류로 이어질 수 있었음 | `@NotNull`만 있고 `@NotBlank`/`@Size`가 없었음 | `@NotBlank` + `@Size(max=200)` 추가 (DB 컬럼 길이와 일치) |
| `PhotoThemeScoringService`가 AI 응답값이 null이면 NPE 발생 가능 | `Map.of(...)`는 `get(null)` 호출 시 예외를 던짐(HashMap과 다른 동작) | 분류값(activity/weather/time/place) null 체크를 먼저 하도록 방어 코드 추가 |
| 비운영 환경 기본값이 약한 DB 비밀번호/와일드카드 CORS로 폴백 | 배포 시 `SPRING_PROFILES_ACTIVE=prod`를 깜빡하면 위험한 기본값이 그대로 살아있었음 | `application-prod.properties`를 분리해 운영 프로파일에서는 기본값 없이 필수 환경변수로만 동작하도록 fail-closed 처리 |

---

## 2. 기능 스코프 변경에 따른 정리

MVP를 "사진 기반 추천만" 지원하도록 범위를 좁히기로 결정한 뒤, 관련 코드를 반쪽만 지워서 생긴 불일치를 발견하고 정리했다.

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| 장르 추천 API(`/recommendations/genre`)는 없앴는데, 플레이리스트 **저장** API로는 여전히 "장르 기반"으로 저장 가능했음 | 추천 엔드포인트만 지우고 저장 쪽 검증 로직은 그대로 둠 | 저장 요청의 `sourceType`이 `PHOTO_RECOMMENDATION`이 아니면 즉시 거부하도록 변경. 그에 딸려있던 `SOURCE_GENRE_REQUIRED`/`GENRE_MISMATCH`/`INVALID_GENRE` 에러 코드도 쓸모없어져서 같이 제거 |

---

## 3. 팀원 API 명세서와의 정합성 문제

팀원이 개발 전 작성해둔 API 명세서와 실제 구현이 여러 곳에서 어긋나 있던 것을 발견해 명세서 기준으로 맞췄다. 프론트가 명세서를 보고 먼저 개발되어 있었기 때문에, 코드를 명세서에 맞추는 방향으로 결정했다.

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| 사진 업로드 파일 필드명이 `photo`인데 명세서는 `image` | 개발 중 임의로 이름을 정함 | `image`로 통일 |
| 저장 요청을 폼 필드로 개별 바인딩했는데, 명세서는 `request`(JSON)+`image`(File) 구조 | 구현 편의상 `@ModelAttribute`로 처리 | `@RequestPart("request")`로 JSON 파트 바인딩하도록 변경 |
| `sourceType` 값이 `PHOTO`/`GENRE`인데 명세서는 `PHOTO_RECOMMENDATION`/`GENRE_RECOMMENDATION` | 축약해서 씀 | enum 값 자체를 명세서와 동일하게 변경 |
| 추천 응답에 `title`이 아예 없어서, 저장할 때마다 프론트가 제목을 직접 입력해야 했음 | 제목 생성 로직 자체가 없었음 | 사진 기반은 GPT 호출 시 제목도 함께 산출, 장르 기반은 템플릿(`오늘의 {장르} 플레이리스트`)으로 생성 |
| 사진 분석 응답에 `place`/`activity`/`weather`/`time`이 아예 없음 — AI가 테마를 곧바로 산출 | 팀원이 설계해둔 "4종 분류 → 고정 규칙표로 테마 변환" 알고리즘이 구현 안 되어 있었음 | 4종 분류 enum 추가, GPT 프롬프트/스키마를 4종 분류 산출로 변경, 팀원이 정의한 점수 규칙표(활동/날씨/시간대/장소별 가중치 + 분위기 보조 규칙)를 그대로 구현 |
| 분석 응답이 `analyzedMood`라는 flat 구조인데, 명세서는 `analysis: { ..., mood: {...} }` 중첩 구조 | 내부 계산용 구조를 응답에 그대로 노출 | 내부 계산 모델은 그대로 두고, API 응답 전용 DTO(`PhotoAnalysisResponse`)를 따로 만들어 명세서 구조에 맞춤 |
| 응답 필드명 다수 불일치 (`musics`↔`musicList`, `analyzedThemes`↔`convertedThemes`, `coverUrl`↔`coverImageUrl`) | 개발 중 임의 작명 | 명세서 필드명으로 전부 리네이밍 |
| 목록 조회가 배열을 그대로 반환하는데, 명세서는 `{"playlists":[...]}`로 감싸야 함 | 래핑 로직 없음 | 래퍼 DTO(`PlaylistListResponse`) 추가 |
| 각 음악 응답에 `position`(순서) 필드가 없음 | 정렬용으로만 쓰고 응답엔 안 내려줌 | `MusicResponse`에 `position` 추가, 플레이리스트 저장 시 position도 명세서대로 1부터 시작하도록 수정(기존엔 0부터) |
| 장르 추천 시 5곡 미만이면 있는 만큼만 조용히 반환 (항상 5곡이어야 한다는 전제 위반) | 0곡일 때만 에러 처리하고 1~4곡 케이스를 놓침 | 5곡 미만이면 `422 INSUFFICIENT_MUSIC` 반환하도록 수정 (이후 장르 기능 자체를 스코프 아웃하며 제거) |
| 에러 코드 이름/상태코드 전반이 명세서와 다름(`UNAUTHORIZED`↔`USER_ID_REQUIRED` 등) | 개발 중 임의 작명 | 명세서 기준으로 이름·상태코드 전부 재정렬 |
| CORS가 모든 헤더/메서드를 허용(`*`, PUT/DELETE 포함) | 개발 편의상 넓게 열어둠 | 명세서 그대로 `Content-Type, X-User-Id` / `GET, POST, OPTIONS`로 좁힘 |

---

## 4. 배포 준비 과정에서 발견한 문제

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| Dockerfile에서 Linux 빌드 단계 중 `Permission denied` 가능성 | Windows에서 체크아웃한 `gradlew` 스크립트에 실행 권한 비트가 없을 수 있음 | 빌드 스테이지에 `chmod +x gradlew` 추가 |

---

## 5. 알고 있지만 의도적으로 보류한 문제

전부 고치는 게 능사는 아니라고 판단해, 우선순위를 따져 일부러 남겨둔 것들이다.

| 문제 | 왜 보류했나 |
| --- | --- |
| S3에 사진 업로드 후 DB 저장이 실패하면, 이미 업로드된 파일이 정리되지 않고 남음 | 제대로 고치려면 보상 트랜잭션/아웃박스 패턴 같은 분산 트랜잭션 처리가 필요해 학부생 프로젝트 규모를 넘어선다고 판단 |
| 관리자가 음원 캐시를 리로드하는 도중 아주 짧은 순간 조회 결과가 어긋날 수 있음(캐시 필드 3개가 원자적으로 갱신되지 않음) | 관리자가 리로드 버튼을 누르는 그 찰나에 동시 요청이 와야만 발생하는 극히 낮은 확률의 상황이라 낮은 우선순위로 분류 |
| 로컬(Windows, 한글 경로)에서 `./gradlew test` 실행 시 `ClassNotFoundException` 발생 | 클래스 파일은 정상 생성되는데 테스트 워커의 classpath 처리 과정에서만 실패해, 프로젝트 경로에 포함된 한글(`김상진`)로 인한 Gradle 테스트 워커의 인코딩 문제로 추정. `compileJava`/`compileTestJava`는 항상 정상이라 코드 자체엔 문제가 없음을 확인했고, 로컬 환경 이슈라 배포 환경(CI, 가비아 서버)에서는 발생하지 않을 가능성이 높아 보류 |
| 폰카메라로 세로로 찍은 사진이 AI 분석 시 옆으로 누운 채로 전달될 수 있음 | `OpenAiVisionService.resizeToJpeg()`가 EXIF 방향(Orientation) 태그를 무시하고 픽셀만 리사이즈함. S3에 저장되는 원본 커버는 EXIF가 유지되어 화면 표시는 대체로 정상이지만, AI에게 전달되는 리사이즈본은 실제로 회전된 상태일 수 있음 | 실제 배포 환경에서 종단 테스트(로컬 프론트 + 배포 백엔드 + 실제 사진)로 정상 동작을 확인했으나, 폰카메라 세로 촬영 케이스는 별도 검증 필요. 해결 방향(프롬프트 지시 vs 서버단 EXIF 회전 보정)은 프론트 팀과 논의 후 결정 예정이라 보류 |
| 저장한 플레이리스트를 삭제하는 API가 없음 | 원 API 명세서와 프론트 구현 모두에 삭제 기능이 없었음 | `DELETE /playlists/{id}` 구현 완료 (아래 6절 참고) |

---

## 6. 회원가입/로그인/삭제 API 추가 및 HTTPS 배포 과정에서 발견한 문제

MVP를 "X-User-Id 헤더 임시 인증"에서 "실제 아이디+비밀번호 인증"으로 확장하고, 백엔드를 HTTPS로 전환하는 과정에서 발견·수정한 문제들이다.

### 6-1. 배포/인프라

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| 프론트(HTTPS, Vercel) → 백엔드(HTTP) 요청이 브라우저에서 전부 차단됨(Mixed Content) | 백엔드가 평문 HTTP로만 배포되어 있었음 | 도메인 구매 없이 `1-201-116-122.nip.io`(IP 기반 무료 서브도메인) + Nginx 리버스 프록시 + Let's Encrypt로 HTTPS 전환. 기존 HTTP(8080) 직접 접근도 그대로 유지해 롤백 경로 확보 |
| 프론트가 실제로는 항상 `http://localhost:8080`으로 요청을 보내고 있었음 | Vercel 프로젝트에 `NEXT_PUBLIC_API_BASE_URL` 환경변수 자체가 설정된 적이 없어서, Next.js 빌드 시 코드가 치환되지 않고 기본값(`localhost:8080`)으로 항상 폴백됨 | 프론트 팀에 Vercel 환경변수 설정 요청(백엔드에서 고칠 수 있는 부분 아님) |
| 2MB 이상 사진 업로드가 전부 실패(폰 사진 대부분이 여기 해당) | Nginx의 기본 업로드 제한(`client_max_body_size` 기본값 1MB)이 앱의 실제 제한(20MB)보다 먼저 걸려서, 요청이 앱까지 도달하지도 못함. 게다가 이 경우 앱의 JSON 에러가 아니라 Nginx의 생 HTML 에러 페이지가 나감 | `client_max_body_size 25M`로 설정(앱 제한 20MB보다 살짝 크게 잡아, 20MB 이하는 전부 앱까지 도달시켜 앱의 `FILE_TOO_LARGE` JSON 에러로 일관되게 응답하도록 함) |
| 새로 배포된 프론트 도메인(`soundscape-front-seven.vercel.app`)에서 오는 요청이 전부 CORS로 막힘 | `CORS_ALLOWED_ORIGINS`에 예전 도메인(`soundscape-front.vercel.app`)만 등록돼 있었음 | `.env`에 새 도메인 추가 후 컨테이너 재시작 |
| `DELETE /playlists/{id}` 배포 직후 프론트에서 호출하면 CORS로 막힐 뻔함 | `WebConfig`의 `allowedMethods`가 `GET, POST, OPTIONS`뿐이었고 `DELETE` 추가를 깜빡함(실행 문서 작성 시점의 누락) | 배포 전 실제 HTTPS 엔드포인트에 OPTIONS 프리플라이트를 직접 날려서 발견, `allowedMethods`에 `DELETE` 추가 |
| 운영 DB에 이미 있던 `demo_user`(id=1) 계정에 로그인 정보를 부여하는 마이그레이션 중 비밀번호 해시가 깨짐 | SSH 원격 명령 문자열 안에 `$2b$10$...` 형태의 BCrypt 해시를 넣었더니, 여러 겹의 쉘 이스케이프(로컬 bash → SSH → docker exec → mysql -e) 과정에서 `$` 뒤의 변수처럼 보이는 부분이 사라짐 | SQL을 로컬 파일로 작성한 뒤 `docker exec -i mysql ... < file.sql`로 stdin 리다이렉션해서 쉘 변수 치환을 아예 거치지 않도록 재실행 |

### 6-2. 코드 (실제 배포 서버에서 재현·수정)

| 문제 | 원인 | 해결 |
| --- | --- | --- |
| 같은 아이디로 정확히 동시에 회원가입 요청 2개를 보내면 하나가 `409 DUPLICATE_USERNAME`이 아니라 `500 INTERNAL_ERROR`로 응답함(실제로 재현·확인함) | `AuthService.signup()`이 `existsByUsername()` 체크와 실제 INSERT 사이에 시간차가 있는 TOCTOU 경쟁 조건. DB의 `UNIQUE KEY uk_users_username` 위반(`DataIntegrityViolationException`)을 `GlobalExceptionHandler`가 별도로 잡지 않아 일반 500으로 노출됨 | 확률이 매우 낮고(정확히 같은 타이밍이어야 함) 해커톤 시연 중 발생 가능성이 없다고 판단해 **의도적으로 보류**(5절과 동일한 성격의 트레이드오프) |
| `request` 멀티파트 파트를 `application/json`이 아닌 다른 Content-Type으로 보내면 `500 INTERNAL_ERROR` | `HttpMediaTypeNotSupportedException`이 `GlobalExceptionHandler`의 세부 핸들러에 안 잡히고 일반 예외 핸들러로 떨어짐 | 프론트 코드(`api.ts`)가 항상 `application/json`을 명시해서 보내므로 실제로는 이 경로를 탈 수 없음을 확인. 방어 코드 공백으로만 기록하고 보류 |
| `S3Service.presignedUrl()`에 예외 처리가 전혀 없음(다른 S3 메서드는 전용 에러코드나 로그 처리가 있음) | 목록/상세/저장/추천 등 거의 모든 읽기 경로에서 호출되는데, S3/자격증명 문제 발생 시 뭉뚱그려진 500으로만 응답 | 발생 확률이 매우 낮고(같은 자격증명으로 이 세션 내내 수십 회 정상 동작 확인) 시연 중 재현 가능성이 없다고 판단해 보류 |
| 음원이 캐시에서 사라지면(현재는 음원 삭제 기능 자체가 없어 불가능) 플레이리스트 상세에서 조용히 빠짐 | `PlaylistService.toDetail()`이 `musicCache.findById()`가 `null`이면 그냥 필터링해서 제외 | 현재 코드 경로상 트리거 불가능해 대응 보류. 추후 음원 삭제 기능이 생기면 재검토 필요 |

### 6-3. 실사용 흐름 종단 테스트 (배포 서버, 실제 사진 사용)

회원가입 → 로그인 → 사진 기반 추천(GPT-4o Vision 실제 호출) → 플레이리스트 저장(S3 커버 업로드) → 목록/상세 조회 → Presigned URL 실제 다운로드(커버·오디오) → 삭제(DB + S3 커버 삭제) 전 구간을 실제 사진 파일로 검증 완료. 에러 코드 18종(인증/검증/파일크기/rate limit 등)도 배포 서버에 직접 요청을 보내 전부 의도한 상태코드·메시지로 응답하는 것을 확인했다.
