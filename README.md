# 카페 메뉴 관리 서비스 (Grids & Circle)

Java 21 · Spring Boot 4.1 · Gradle · MySQL · JPA. 팀 7: 유승민, 김지호, 김아리, 안형빈.

## 실행
1. DB 준비: 로컬 MySQL(8.0.16 이상, 포트 3306)에서 `docs/mysql-setup.sql`을 root로 한 번 실행
   - DB `cafe` / 사용자 `cafe` / 비밀번호 `cafe1234` — 팀 공통, `application.yml` 기본값과 같음
2. `./gradlew bootRun` → API `http://127.0.0.1:8080` (시작 시 `schema.sql`, `data.sql` 적용)
3. 화면: `cd web && java -m jdk.httpserver -b 127.0.0.1 -p 8000` → "실제 API 연결", 서버 주소 `http://127.0.0.1:8080`

## 구조
`global`(공통 오류·CORS·Clock) · `category` · `menu` · `order` · `delivery`(담당자가 각자 브랜치에서 추가) — 도메인별 패키지, 담당은 기획서 8장 참고.
DB 설계: [docs/db-design.md](docs/db-design.md) · 요청 검증: `requests/api-check.http`

## 시각 경계(13:59:59 / 14:00:00) 확인
시계 주입용 `Clock` 빈(`global/config/ClockConfig`)을 사용하고, 테스트에서 `Clock.fixed(...)`로 바꿔 확인한다. (작성 예정)
