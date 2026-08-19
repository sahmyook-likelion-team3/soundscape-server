# 백엔드 HTTPS 전환 — 실행 문서 (Nginx + Let's Encrypt + nip.io)

> 이 문서는 작업 도중 세션이 끊기더라도 다른 AI(예: 안티그래비티+Gemini)가 이어서 작업할 수 있도록,
> 배경/원인/단계별 명령어를 전부 포함해 작성했습니다. **추측하지 말고 아래 내용을 순서대로 그대로 실행하세요.**

## 0. 배경 및 목표

- **문제**: 백엔드가 `http://1.201.116.122:8080`으로 배포되어 있고(HTTP), 프론트는 `https://soundscape-front.vercel.app`(HTTPS)에 배포되어 있습니다. 브라우저는 HTTPS 페이지에서 HTTP로 나가는 요청(Mixed Content)을 기본적으로 차단합니다. 실제로 안드로이드 Chrome에서 사진 촬영 후 추천 API를 호출하면 `Failed to fetch` 에러가 발생하는 것을 실기기 스크린샷으로 확인했습니다(서버 자체는 `curl`로 직접 호출 시 정상 응답, 200 OK — 즉 서버가 죽은 게 아니라 브라우저가 요청 자체를 차단한 것으로 확인됨).
- **목표**: 도메인을 새로 구매하지 않고, 백엔드를 HTTPS로 전환합니다. 무료 IP 기반 서브도메인 서비스(nip.io) + Let's Encrypt 무료 인증서 + Nginx 리버스 프록시를 사용합니다. **비용 발생 없음.**
- **범위**: 이 문서는 **백엔드 서버(가비아 클라우드) 작업만** 다룹니다. 작업 완료 후 프론트 쪽에서 API base URL 환경변수(`NEXT_PUBLIC_API_BASE_URL`)를 `https://1-201-116-122.nip.io`로 바꿔야 실제로 연동이 완성되는데, **프론트 코드는 이 작업 범위가 아니므로 프론트 담당자에게 별도로 전달**해야 합니다 (이 프로젝트의 확립된 규칙: 프론트 저장소는 절대 직접 건드리지 않음).

## 1. 서버 접속 정보

```
Host: 1.201.116.122
User: ubuntu
SSH Key: C:\Users\김상진\SSH_KeyPair-260819011102.pem
OS: Ubuntu 22.04
```

접속 명령 (로컬 Windows에서, Git Bash 기준):

```bash
ssh -i "/c/Users/김상진/SSH_KeyPair-260819011102.pem" ubuntu@1.201.116.122
```

PowerShell에서는:

```powershell
ssh -i "C:\Users\김상진\SSH_KeyPair-260819011102.pem" ubuntu@1.201.116.122
```

이 서버는 **8/28(금) 23:59에 삭제 예정**이므로, 인증서 갱신(90일 주기)까지 신경 쓸 필요는 없습니다. 1회 발급만 되면 해커톤 기간 동안은 충분합니다.

## 2. 사전 확인 (실행 전 필수)

SSH 접속 후, 현재 배포 상태를 먼저 확인합니다. **아래 명령 결과를 보고 이후 단계의 컨테이너 이름/포트가 실제와 일치하는지 확인한 뒤 진행하세요** (이 문서 작성 시점 기준 추정값이며, 실제 서버 상태와 다를 수 있음):

```bash
sudo docker ps
```

예상되는 출력: `soundscape`(또는 유사한 이름) 앱 컨테이너가 `8080:8080`으로, `soundscape-mysql` 컨테이너가 `soundscape-net` 네트워크로 연결되어 있어야 합니다 (`README.md` 배포 섹션 기준). 앱 컨테이너가 8080 포트를 리스닝하고 있는지 아래로도 확인:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/playlists -H "X-User-Id: 1"
```

`200`이 나와야 정상입니다 (로컬 머신에서 이미 같은 방식으로 확인해서 200을 받았습니다).

방화벽/보안그룹에서 **443 포트(HTTPS)와 80 포트(Let's Encrypt 인증 챌린지용)가 인바운드로 열려있는지** 가비아 클라우드 콘솔에서 확인하세요. 8080은 이미 열려있을 것이므로 그건 그대로 두면 됩니다(Nginx 프록시 적용 후에도 기존 8080 직접 접근은 그대로 살려둘 것이므로, 프론트가 잘못되면 즉시 롤백 가능).

## 3. nip.io 서브도메인 확인

별도 등록 절차가 필요 없습니다. IP `1.201.116.122`를 하이픈으로 바꾼 형태의 서브도메인이 자동으로 그 IP를 가리킵니다:

```
1-201-116-122.nip.io  →  1.201.116.122
```

로컬에서 미리 확인:

```bash
nslookup 1-201-116-122.nip.io
# 또는
ping -n 1 1-201-116-122.nip.io
```

`1.201.116.122`로 정상 resolve되면 준비 완료입니다.

## 4. Nginx 설치

서버(SSH 접속 상태)에서:

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

설치 확인:

```bash
sudo systemctl status nginx
```

`active (running)`이면 정상입니다.

## 5. Nginx 리버스 프록시 설정 (HTTP, 인증서 발급 전 단계)

Certbot이 인증서를 발급하려면 먼저 해당 도메인에 대한 Nginx 서버 블록이 80번 포트에 존재해야 합니다. 아래 파일을 생성합니다:

```bash
sudo nano /etc/nginx/sites-available/soundscape
```

내용:

```nginx
server {
    listen 80;
    server_name 1-201-116-122.nip.io;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**중요**: `proxy_set_header X-Real-IP $remote_addr;`와 `X-Forwarded-For`를 반드시 넣어야 합니다. 백엔드의 `PlaylistController`가 사진 추천 API에 IP 기반 rate limit(분당 10회)을 걸고 있는데, `RateLimiter`가 `request.getRemoteAddr()`만 사용하도록 되어 있어(코드 확인됨 — `TROUBLESHOOTING.md`에 이미 "X-Forwarded-For를 신뢰하면 우회 가능해서 일부러 안 씀"이라고 기록되어 있음), Nginx를 거치면 모든 요청이 Nginx의 로컬 IP(127.0.0.1)로 잡혀서 **전체 사용자가 하나의 IP로 묶여 분당 10회 제한을 공유하게 됩니다**. 이 부분은 6-1절에서 백엔드 쪽 대응이 필요합니다(코드 수정 필요 — 반드시 사용자 승인 후 진행).

심볼릭 링크 활성화:

```bash
sudo ln -s /etc/nginx/sites-available/soundscape /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

`nginx -t` 결과가 `syntax is ok` / `test is successful`이어야 합니다.

동작 확인 (아직 HTTP):

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://1-201-116-122.nip.io/api/v1/playlists -H "X-User-Id: 1"
```

`200`이 나오면 프록시가 정상 동작하는 것입니다.

## 6. Let's Encrypt 인증서 발급 (Certbot)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 1-201-116-122.nip.io
```

대화형 프롬프트가 나오면:
- 이메일 입력 (알림 수신용, 아무 이메일이나 가능)
- 약관 동의: `Y`
- 이메일 뉴스레터 수신 여부: `N` (선택)
- HTTP를 HTTPS로 자동 리다이렉트할지 물으면 → **"2) Redirect"를 선택하지 말고 "1) No redirect"를 선택**하세요. 기존 프론트가 여전히 `http://1.201.116.122:8080`으로 직접 요청을 보낼 수도 있으므로(프론트 팀이 URL을 아직 안 바꿨을 경우), 강제 리다이렉트를 걸면 그 요청들이 깨질 수 있습니다. HTTPS는 추가로 여는 것이고, 기존 HTTP 접근은 그대로 살려둡니다.

성공하면 Certbot이 `/etc/nginx/sites-available/soundscape`를 자동으로 수정해서 443 포트 설정과 인증서 경로를 추가합니다. 결과 확인:

```bash
sudo cat /etc/nginx/sites-available/soundscape
```

`listen 443 ssl;`과 `ssl_certificate`, `ssl_certificate_key` 줄이 추가되어 있어야 합니다.

최종 동작 확인 (HTTPS):

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://1-201-116-122.nip.io/api/v1/playlists -H "X-User-Id: 1"
```

`200`이 나오면 완료입니다. 로컬 PC 브라우저에서도 `https://1-201-116-122.nip.io/api/v1/playlists`로 직접 접속해서 자물쇠 아이콘(유효 인증서)이 뜨는지 확인하세요.

## 6-1. (선택, 코드 수정 필요 — 반드시 승인 후 진행) Rate Limiter가 실제 클라이언트 IP를 보도록 수정

5절에서 언급한 대로, Nginx 프록시를 거치면 `request.getRemoteAddr()`가 항상 Nginx의 IP(127.0.0.1)를 반환해서 rate limit이 왜곡됩니다. 고치려면 `PlaylistController.java`에서 IP를 가져오는 부분을 `X-Forwarded-For` 헤더 우선으로 바꿔야 하는데, 이건 이전에 `TROUBLESHOOTING.md`에 기록된 대로 "헤더를 신뢰하면 우회 가능"이라는 보안 트레이드오프가 있습니다.

**권장 방법**: 헤더를 무조건 신뢰하는 게 아니라, Nginx가 프록시로써 보낸 요청만 신뢰하도록 Spring에 `ForwardedHeaderFilter`를 등록하면, `request.getRemoteAddr()` 자체가 프록시 체인을 거쳐 원본 클라이언트 IP를 반환하도록 처리됩니다(Nginx가 설정한 `X-Forwarded-For`를 Spring이 신뢰 가능한 방식으로 해석). 이 방식은 클라이언트가 헤더를 직접 위조해도 Nginx가 그 위에 실제 소켓 IP를 이어붙이는 방식(`X-Forwarded-For` 체이닝)이라 우회가 어렵습니다.

이 작업은 **해커톤 시연에는 필수가 아닙니다** (rate limit이 다소 부정확해지는 정도의 영향이며, 서비스 자체가 안 되는 건 아님). 시간이 없으면 스킵하고 넘어가도 됩니다. 진행하고 싶다면 프로젝트 소유자에게 먼저 알리고 승인받은 뒤 코드를 수정하세요.

## 7. 백엔드 CORS 설정 확인/갱신

`CORS_ALLOWED_ORIGINS` 환경변수(서버의 `.env` 파일, `docker run --env-file .env`로 주입됨)에 프론트 도메인이 정확히 들어있는지 확인합니다:

```bash
cat ~/.env | grep CORS_ALLOWED_ORIGINS
```

`CORS_ALLOWED_ORIGINS=https://soundscape-front.vercel.app` 형태여야 합니다(오타, 트레일링 슬래시, 프로토콜 불일치 없이). **nip.io 도메인 자체는 CORS와 무관합니다** — CORS는 "프론트가 어느 origin에서 요청을 보내는지"를 검사하는 것이고, 백엔드가 어떤 도메인으로 서빙되는지와는 별개입니다. 즉 이 설정은 이미 되어 있어야 하며, 이번 HTTPS 작업으로 인해 변경할 필요는 없습니다. 값이 비어있거나 틀렸다면 그것만 별도로 수정 후:

```bash
sudo docker restart <앱 컨테이너 이름>
```

(컨테이너 이름은 2절에서 확인한 `docker ps` 결과 기준으로 채워넣으세요.)

## 8. 완료 후 프론트에 전달할 내용

작업이 끝나면 프론트 담당자에게 아래 내용을 전달합니다 (프론트 코드 수정은 이 문서의 범위가 아니며, 프론트 담당자가 직접 진행):

```
백엔드 API base URL을 아래로 변경해주세요:
기존: http://1.201.116.122:8080
변경: https://1-201-116-122.nip.io

Vercel 환경변수 NEXT_PUBLIC_API_BASE_URL 값을 위 HTTPS 주소로 바꾸고 재배포하면 됩니다.
API 경로 구조(/api/v1/...)는 동일합니다.
```

## 9. 롤백 방법

문제가 생기면 아래 중 하나로 즉시 원상복구할 수 있습니다. **기존 `http://1.201.116.122:8080` 직접 접근은 이 작업 내내 한 번도 끊기지 않으므로, 최악의 경우에도 서비스 중단 없이 되돌릴 수 있습니다.**

```bash
# Nginx만 끄기 (가장 간단, 즉시 원복)
sudo systemctl stop nginx

# 완전히 제거하고 싶은 경우
sudo apt remove --purge nginx nginx-common certbot python3-certbot-nginx -y
sudo rm -rf /etc/nginx
```

Docker 컨테이너(앱, MySQL)는 이 작업 과정에서 전혀 건드리지 않으므로 위 롤백과 무관하게 항상 정상 동작합니다.

## 10. 체크리스트

- [ ] SSH 접속 확인, `docker ps`로 현재 컨테이너 이름/포트 실제 값 확인 (이 문서의 추정값과 다르면 이후 명령어의 컨테이너 이름을 실제 값으로 교체)
- [ ] 가비아 클라우드 보안그룹에서 443, 80 포트 인바운드 허용 확인
- [ ] `1-201-116-122.nip.io`가 `1.201.116.122`로 정상 resolve되는지 로컬에서 확인
- [ ] Nginx 설치 및 기동 확인
- [ ] `/etc/nginx/sites-available/soundscape` 생성, `X-Real-IP`/`X-Forwarded-For` 헤더 전달 설정 포함, `nginx -t` 통과
- [ ] Certbot으로 인증서 발급, **"No redirect" 선택** (강제 리다이렉트 걸지 않음)
- [ ] `https://1-201-116-122.nip.io/api/v1/playlists` curl 및 브라우저에서 정상 응답/유효 인증서 확인
- [ ] `CORS_ALLOWED_ORIGINS` 값에 프론트 Vercel 도메인이 정확히 들어있는지 확인 (이번 작업으로 변경 불필요, 확인만)
- [ ] (선택) rate limiter의 IP 인식 문제 — 시간 되면 6-1절 진행, 승인 필요
- [ ] 프론트 담당자에게 새 HTTPS base URL 전달 (8절 문구 그대로 사용 가능)
- [ ] 실기기(안드로이드 Chrome 등)에서 새 URL로 실제 사진 추천까지 end-to-end 테스트
