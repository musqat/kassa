# Kassa

결제 연동을 중심에 둔 쇼핑몰
주문·재고·결제가 서로 어긋나지 않게

[![backend](https://github.com/musqat/kassa/actions/workflows/backend.yml/badge.svg)](https://github.com/musqat/kassa/actions/workflows/backend.yml)

<br>

## 진행 상황

지금은 상품 조회까지 있다. 회원·장바구니, 주문과 재고, 결제 순서로 붙인다.

<br>

## 구조

```
kassa/
├── backend/    Kotlin · Spring Boot 4 · JPA · Flyway
│   └── src/main/kotlin/com/kassa/
│       ├── catalog/
│       │   ├── domain/
│       │   ├── repository/
│       │   ├── service/
│       │   ├── controller/
│       │   └── dto/
│       └── common/
│           ├── error/         에러 코드와 ProblemDetail 응답
│           ├── trace/         요청 추적 아이디
│           └── config/        CORS
└── frontend/   Next.js 16 · Tailwind 4
    └── src/
        ├── app/           라우트
        ├── components/    UI
        └── lib/           API 클라이언트, 포맷
```

**에러 응답** — RFC 9457 `ProblemDetail` 에 자체 코드 `code` 와 `requestId` 를 얹는다.

**요청 추적** — 요청마다 식별자를 만들어 응답 헤더 `X-Request-Id`, 로그, 에러 응답에 같은 값으로 넣는다.

<details>
<summary><b>API</b></summary>

<br>

전체 목록과 요청·응답 모양은 Swagger UI 에서 본다. 로컬은 http://localhost:8080/swagger-ui.html, 문서 원본은 `/v3/api-docs`.

| 묶음 | 내용 |
|---|---|
| 상품 | 목록, 단건 |
| 회원 | 가입, 아이디 중복확인, 내 정보, 탈퇴 |
| 인증 | 로그인, 이메일 인증·재전송, 아이디 찾기, 비밀번호 재설정 |

자물쇠 버튼에 로그인으로 받은 토큰을 넣으면 인증이 필요한 API 도 화면에서 호출할 수 있다.

```json
{
  "status": 404,
  "title": "Not Found",
  "detail": "상품을 찾을 수 없습니다",
  "instance": "/api/products/99999",
  "code": "CATALOG_001",
  "requestId": "a1b2c3d4"
}
```

</details>

<br>

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Kotlin 2.3 · Spring Boot 4.1 · Spring Data JPA · Flyway |
| Frontend | Next.js 16 · React 19 · TypeScript · Tailwind CSS 4 |
| Data | PostgreSQL 17 |
| Test | JUnit 5 · MockMvc · Testcontainers |
| CI | GitHub Actions |

<br>

## 테스트

백엔드 **7 개**. 통합 테스트는 Testcontainers 로 PostgreSQL 17 을 띄운다.

CI 는 PR 과 `main` push 마다 백엔드 빌드와 테스트를 돌린다. 프런트는 아직 CI 에 없다.

<br>

## 로컬에서 실행

<details>
<summary><b>실행 방법</b></summary>

<br>

Docker, JDK 21, Node 20 이상이 필요하다.

```bash
docker compose up -d
```

PostgreSQL 이 호스트 5433 으로 뜬다.

```bash
cd backend
./gradlew bootRun
```

```bash
cd frontend
npm install
npm run dev
```

http://localhost:3000 에서 상품 목록이 나온다. 테스트는 `backend` 에서 `./gradlew test` 이고,
Docker 가 켜져 있어야 한다.

</details>
