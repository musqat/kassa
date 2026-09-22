# Kassa

결제 연동을 중심에 둔 쇼핑몰. 이름은 네덜란드·독일어권에서 계산대를 뜻한다.

주문·재고·결제의 정합성이 이 프로젝트의 본체다. 쇼핑몰 화면은 그 흐름이 실제로 도는 자리로 쓴다.

## 진행 상황

| 단계 | 내용 | 상태 |
|---|---|---|
| M1 | 상품 조회 API, 에러 코드 체계, 요청 추적, CI, 상품 목록 화면 | 완료 |
| M2 | 회원·로그인, 장바구니 | |
| M3 | 주문 생성, 재고 선점, 동시 주문 처리 | |
| M4 | 결제 사가와 보상, 테스트용 결제 게이트웨이 | |
| 배포 | | |
| M5 | 포트원 연동 | |
| M6 | 웹훅 인박스 | |
| M7 | 취소·환불 | |

## 기술 스택

- 백엔드: Kotlin, Spring Boot 4, Spring Data JPA, PostgreSQL 17, Flyway
- 프런트: Next.js 16, React 19, TypeScript, Tailwind CSS 4
- 테스트: JUnit 5, Testcontainers, MockMvc
- CI: GitHub Actions

## 로컬 실행

Docker, JDK 21, Node 20 이상이 필요하다.

데이터베이스

```bash
docker compose up -d
```

호스트 포트는 5433 이다.

백엔드

```bash
cd backend
```

```bash
./gradlew bootRun
```

`http://localhost:8080/api/products` 에서 상품 목록을 확인할 수 있다.

프런트

```bash
cd frontend
```

```bash
npm install
```

```bash
npm run dev
```

`http://localhost:3000`

테스트

```bash
cd backend
```

```bash
./gradlew test
```

Testcontainers 가 PostgreSQL 을 띄우므로 Docker 가 켜져 있어야 한다.

## 구조

```
backend/src/main/kotlin/com/kassa/
├─ catalog/       상품·카테고리 (domain · repository · service · controller · dto)
└─ common/
   ├─ error/      에러 코드와 ProblemDetail 응답
   ├─ trace/      요청 추적 아이디
   └─ config/     CORS
```

도메인으로 먼저 나누고 그 안에서 계층으로 나눈다. M2 부터 `member`, `cart`, `order`, `payment`, `saga` 가 같은 모양으로 붙는다.

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/products?categoryId=` | 노출 대상 상품 목록 |
| GET | `/api/products/{id}` | 상품 단건 |

에러는 RFC 9457 `ProblemDetail` 로 응답하고, 자체 코드 `code` 와 추적용 `requestId` 가 붙는다.

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

모든 응답 헤더에 같은 값의 `X-Request-Id` 가 들어가고, 서버 로그에도 같은 값이 찍힌다.
