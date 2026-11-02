# DB 설계

DDL은 `src/main/resources/schema.sql`, 초기 데이터는 `data.sql`. 스키마 변경은 D(유승민)에게 요청한다.
고객·배송 묶음 테이블은 두지 않고, 주문의 이메일·배송일로 조회해 만든다.

```
category 1 ──< menu 1 ──< order_item >── 1 orders
```

| 테이블 | 주요 컬럼 | 관계·제약 |
|---|---|---|
| category | id, name(≤30) | name 유일, 대소문자 구분 |
| menu | id, category_id, name(≤100), price, description(≤300), available | category_id → category.id, 신규는 available=true, price 1~1,000,000 |
| orders | id, email, address, postcode(5), created_at, shipping_date, total_amount, status, cancelled_at, memo | status는 PLACED/CANCELLED, PLACED만 cancelled_at이 null, email 대소문자 구분 |
| order_item | id, order_id, menu_id, menu_name, unit_price, quantity, line_amount | order_id → orders.id, menu_id → menu.id, (order_id, menu_id) 유일, 수량 1~20, line_amount = unit_price × quantity |

## 지킬 점
- `menu_name`, `unit_price`는 접수 당시 값을 복사해 저장한다. 이후 메뉴가 바뀌어도 주문·통계는 유지된다.
- `shipping_date`는 접수 시각(14:00:00 규칙)으로 한 번 계산해 저장하고 바꾸지 않는다.
- 통계 기준일은 `DATE(created_at)`(서울 기준 접수일), 배송 묶음은 `shipping_date`+`email`.
- 이메일·카테고리명은 `utf8mb4_bin`이라 대소문자를 구분한다.
- 외래키는 안전장치다. 삭제 전에 서비스에서 먼저 확인해 409로 응답한다.
  - 메뉴가 있는 카테고리 → `CATEGORY_IN_USE`
  - order_item이 참조한 메뉴(취소 포함) → `MENU_IN_USE`
- 현재 `ddl-auto: none`. 엔티티가 모이면 `validate`로 바꿔 스키마와 일치하는지 확인한다.
