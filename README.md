# 카페 메뉴 관리 서비스 (Grids & Circle)

Java 21 · Spring Boot 4.1 · Gradle · MySQL · JPA · Redis. 팀 7: 유승민, 김지호, 김아리, 안형빈.

## 실행
1. DB 준비: 로컬 MySQL(8.0.16 이상, 포트 3306)에서 `docs/mysql-setup.sql`을 root로 한 번 실행
   - DB `cafe` / 사용자 `cafe` / 비밀번호 `cafe1234` — 팀 공통, `application.yml` 기본값과 같음
   - 포트나 계정이 다르면 프로젝트 루트에 `.env`를 만든다(Git에 올라가지 않음). `properties` 형식이라 `KEY=값`으로만 쓰고 따옴표·`export`는 쓰지 않는다. 예: `DB_URL=jdbc:mysql://localhost:3310/cafe?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true`, 계정이 다르면 `DB_USERNAME`, `DB_PASSWORD`도 같은 방식으로 적는다.
   - `./gradlew test`의 리포지토리 테스트도 이 DB 설정을 쓰므로 MySQL이 떠 있어야 한다.
2. Redis 준비: 로컬 Redis를 `127.0.0.1:6379`로 실행 (예: `docker run -d --name redis -p 6379:6379 redis:7`)
   - 메뉴 목록(A06)을 캐시하므로, Redis가 꺼져 있으면 A06이 500이다.
3. `./gradlew bootRun` → API `http://127.0.0.1:8080` (시작 시 `schema.sql`, `data.sql` 적용)
4. 화면: `cd web && java -m jdk.httpserver -b 127.0.0.1 -p 8000` → "실제 API 연결", 서버 주소 `http://127.0.0.1:8080`

## 구조
`global`(공통 오류·CORS·Clock·캐시) · `category` · `menu` · `order` · `delivery` · `reference`(외부 조회) — 도메인별 패키지, 담당은 기획서 8장 참고.
DB 설계: [docs/db-design.md](docs/db-design.md) · 요청 검증: `requests/api-check.http`

## 개발 환경
| 항목 | 버전 |
|---|---|
| Java | 21 (Gradle toolchain) |
| Spring Boot | 4.1.2 |
| 빌드 | Gradle (`./gradlew`) |
| MySQL | 8.0.16 이상 (테스트는 8.4에서 실행) |
| Redis | 6379 포트 (메뉴 목록 캐시) |
| 주요 의존성 | webmvc, validation, data-jpa, restclient, cache, data-redis, mysql-connector-j |

## 팀원별 담당
| 팀원 | 담당 | 브랜치 |
|---|---|---|
| 김아리 | 카테고리 API | `feature/category-main` |
| 안형빈 | 메뉴 API | `feature/menu-main` |
| 김지호 | 주문 API (A11·A12·A13·A16·A22·A23) | `feature/order-main` |
| 유승민 | 배송·통계 API (A14·A17·A18·A24), DB 설계, 공통 오류·CORS, README | `feature/delivery-main` |

작업은 각자 `feature/<담당>/<번호>/<작업>` 브랜치에서 하고, 담당별 `*-main`에 합친 뒤 `dev`에 반영한다.

## 평가 대상
- 브랜치: `main` (커밋 해시는 제출 폼에 적는다)

## 검증 방법
- 자동 테스트: `./gradlew test` — 159개 (리포지토리 테스트는 MySQL 필요, Redis는 없어도 된다)
- 수동 요청: `requests/api-check.http` (정상 24 + 오류 케이스 400·404·409)
  - **기준 상태**: 서버(8080)를 띄운 직후의 깨끗한 DB(`data.sql`의 카테고리 1·2, 메뉴 1~4만 있는 상태). ID를 고정한 변수가 있어 다른 데이터가 있으면 결과가 달라진다.
  - **변수**: 파일 맨 위 `@baseUrl`, `@categoryId`, `@menuId`, `@orderId`, `@newMenuId`, `@soldOutMenuId`, 배송·통계 섹션 위 `@statDate`(접수일), `@shipDate`(배송일). 등록 응답의 id를 해당 변수에 넣고 다음 요청을 실행한다.
  - **실행 순서**: 카테고리(A01→A05) → 메뉴(A06→A10, 오류 1~3) → 주문(A11→A16, 오류 4~6) → 배송·통계(A17·A18·A24·A14). 섹션마다 `### 실행 순서`·`### 준비 데이터` 주석이 있다.
  - `@statDate`·`@shipDate`는 실행한 날짜로 바꾼다. 14:00 이후 접수면 배송일은 다음 날이다.

## 선택 심화 구현
필수 API의 경로·상태 코드·응답 형식은 그대로 두고 추가했다.

| 항목 | API / 코드 | 테스트 | 담당 |
|---|---|---|---|
| Redis 메뉴 캐시 | **A06** `GET /api/v1/menus` · `global/config/CacheConfig`, `MenuService` | 수동 확인 (아래) | 김지호 |
| 메뉴 페이징 | `GET /api/v1/menus/page?page=0&size=10` (size 1~50, 판매 중지 포함, ID 오름차순) | `MenuControllerTest` 4개 | 안형빈 |
| RestClient 외부 조회 | `GET /api/v1/reference/posts/{postId}` · `reference/` | `ReferencePostClientTest` 10개, `ReferenceControllerTest` 3개 | 안형빈 |
| 목록 페이징 (팀 추가) | `/delivery-batches/page`, `/statistics/menus/page`, `/statistics/customers/page` | delivery `*Page*Test` | 유승민 |
| | `/orders/page`, `/categories/page` | — | 김지호, 김아리 |
| 업무 규칙 테스트 | 14:00 배송일·취소 마감 (`ShippingPolicy`) | `ShippingPolicyTest` 7개 | 김지호 |

**Redis 캐시 (A06)**
- 캐시 키 `menus::all`, 값은 JSON, TTL **30초** (`CacheConfig.CACHE_TTL`).
- 메뉴 등록·수정·삭제·판매 상태 변경 시 `@CacheEvict`로 비워, 변경 후 첫 조회에 바로 반영된다.
- 확인: A06을 두 번 조회하면 첫 번째만 서버 로그에 SQL이 찍힌다(`spring.jpa.show-sql: true`). 저장 값과 남은 시간은 `redis-cli GET "menus::all"`, `redis-cli TTL "menus::all"`.

**RestClient 외부 조회**
- 기본 외부 주소 `app.outside.base-url: https://jsonplaceholder.typicode.com` (설정으로 변경 가능), 연결 3초 · 읽기 5초 (`spring.http.clients`).
- 실패는 공통 오류 형식(`fieldErrors: {}`)으로 404·502·504를 돌려준다.

## 완료·미완료 기능과 미해결 오류
- **완료**: 필수 API 24개(A01~A24) 전부. 선택 심화 1(업무 규칙 테스트)·3(Redis 캐시)·4(메뉴 페이징)·5(RestClient 외부 조회).
- **미완료**: 선택 심화 2(실제 DB 저장·롤백 테스트)는 구현하지 않았다.
- **미해결 오류**
  - 주문 접수(A11)에서 `items` 배열 안에 `null`이 들어오면(`"items": [null]`) 400이 아니라 500 `INTERNAL_ERROR`를 돌려준다.
  - 같은 이메일·배송일로 주소가 다른 주문이 **동시에** 들어오면 둘 다 접수될 수 있다(주소 검사와 저장 사이에 잠금이 없음). 순차 요청은 409로 막힌다.
- **알려진 제약**: Redis가 꺼져 있으면 메뉴 목록(A06)이 500이다(실행 2단계 참고).

## 담당 작업·검증 (김아리 — 카테고리)
- **담당 작업**: 카테고리 API **A01~A05**의 Controller·Service·Repository.
- **테스트**: 24개(서비스 11 · 컨트롤러 10 · 리포지토리 3).
- **상태**: 완료

## 담당 작업·검증 (안형빈 — 메뉴)
- **담당 작업**: 메뉴 API **A06~A10, A15, A19~A21**의 Controller·Service·Repository. 심화: 메뉴 페이징, RestClient 외부 조회.
- **테스트**: 19개(메뉴 컨트롤러 6 · 외부 조회 13).
- **상태**: 완료

## 담당 작업·검증 (김지호 — 주문)
- **담당 작업**: 주문 API **A11·A12·A13·A16·A22·A23**. 심화: Redis 메뉴 캐시, 주문 페이징.
- **테스트**: 20개(서비스 10 · 컨트롤러 3 · 14시 경계 `ShippingPolicyTest` 7).
- **상태**: 완료

## 담당 작업·검증 (유승민)
- **담당 작업**: 배송·통계 API **A14·A17·A18·A24**의 Controller·Service·Repository. 그 밖에 DB 설계([docs/db-design.md](docs/db-design.md)), 공통 오류 응답·CORS, 프로젝트 설정, README. 심화: 배송·통계 페이징(A14·A18·A24).
- **테스트**: 72개(서비스 41 · 컨트롤러 15 · 리포지토리 16).
- **상태**: 완료

## 배송·통계 구현 규칙 (delivery, 유승민)
명세에 없거나 프론트(`web/js/domain.js`)의 응답 검증과 맞물려 정한 규칙.
- **A18 메뉴 통계**: PLACED 수량이 0인 메뉴(그날 취소된 주문에만 있던 메뉴)는 목록에서 **제외**한다. 명세에는 포함 여부가 없고, 프론트는 `totalQuantity > 0`이 아니면 `RESPONSE_MISMATCH`로 처리하기 때문이다.
- **A24 고객 통계**: 모두 취소한 고객도 포함하되 `placedOrderCount`·`totalQuantity`·`totalAmount`는 0, `orderCount`는 취소 포함 건수. 이메일 오름차순.
- **A14 배송 묶음**: PLACED만, 이메일 오름차순. `orderIds`는 오름차순·중복 없음, `items`는 메뉴 ID → 단가 → 메뉴명 오름차순. 같은 이메일·배송일의 PLACED 주문은 접수 시 주소가 같도록 보장되므로(ADDRESS_MISMATCH) 묶음 주소는 첫 주문의 값을 쓴다. 기존 주문이 모두 취소됐다면 새 주소의 주문만 묶음에 나온다. 접수 쪽 검사와 별개로 조회에서도 한 번 더 확인해, 묶음 안의 주소·우편번호가 앞뒤 공백 제거 후 서로 다르면 **경고 로그만 남기고 조회는 성공**시킨다(한 건의 데이터 문제로 그날 배송 목록 전체가 막히지 않도록).
- 합산 키는 (메뉴 ID, 주문 당시 메뉴명, 단가)가 모두 같을 때만 — SQL `GROUP BY` 대신 서비스에서 문자열 그대로 비교한다. 정렬은 메뉴 ID → 단가 → 메뉴명 오름차순.

## 시각 경계(13:59:59 / 14:00:00) 확인
시계 주입용 `Clock` 빈(`global/config/ClockConfig`, 서울 시각)을 사용하고, 테스트에서 `Clock.fixed(...)`로 바꿔 확인한다.
- **배송일**(`ShippingPolicy.shippingDateOf`): 접수가 14:00:00 **전**이면 당일, **14:00:00 정각 이후**면 다음 날이다. 접수 시각으로 한 번 계산해 `orders.shipping_date`에 저장하고 이후 바꾸지 않는다.
- **취소**(`ShippingPolicy.isCancellable`): 배송일 14:00:00 **전까지** 가능하고, 정각부터는 불가하다.
- **확인한 경계**(`ShippingPolicyTest`): 13:59:59(당일) · 14:00:00(다음 날) · 14:00:01(다음 날) · 12/31 14:00(다음 해 1/1) · 배송일 13:59:59 취소 가능 · 배송일 14:00:00 취소 불가.
- **통계(A17·A18·A24)는 14시 경계와 무관**하다. 접수일(`created_at`의 서울 날짜, `[당일 00:00, 다음 날 00:00)`) 기준이라 23:59:59.999999는 당일, 00:00:00은 다음 날로 센다. 배송 묶음(A14)만 저장된 `shipping_date`를 쓴다.
