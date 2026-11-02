-- 스키마는 DB 설계 총괄(D)이 관리한다. 서버를 다시 띄워도 안전하도록 IF NOT EXISTS 사용.
-- 날짜·시각 컬럼은 서울 시각(Asia/Seoul) 기준으로 저장한다. (CHECK 제약은 MySQL 8.0.16+ 에서 동작)

CREATE TABLE IF NOT EXISTS category (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,  -- 대소문자 구분
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    price       INT          NOT NULL,
    description VARCHAR(300) NOT NULL DEFAULT '',
    available   BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_menu_category (category_id),
    KEY idx_menu_name (name),
    CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_menu_price CHECK (price BETWEEN 1 AND 1000000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,  -- 대소문자 구분
    address       VARCHAR(200) NOT NULL,
    postcode      CHAR(5)      NOT NULL,
    created_at    DATETIME(6)  NOT NULL,            -- 접수 시각 (통계 기준일 = 이 날짜)
    shipping_date DATE         NOT NULL,            -- 접수 시각으로 한 번 계산해 저장, 이후 변경 안 함
    total_amount  BIGINT       NOT NULL,            -- 항목 line_amount 합계
    status        VARCHAR(20)  NOT NULL,            -- PLACED | CANCELLED
    cancelled_at  DATETIME(6)  NULL,
    memo          VARCHAR(200) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    KEY idx_orders_email (email),
    KEY idx_orders_batch (shipping_date, email, status),
    KEY idx_orders_created_at (created_at),
    CONSTRAINT ck_orders_status CHECK (status IN ('PLACED', 'CANCELLED')),
    CONSTRAINT ck_orders_cancelled CHECK (
        (status = 'PLACED' AND cancelled_at IS NULL) OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    menu_id     BIGINT       NOT NULL,
    menu_name   VARCHAR(100) NOT NULL,              -- 접수 당시 이름
    unit_price  INT          NOT NULL,              -- 접수 당시 단가
    quantity    INT          NOT NULL,
    line_amount BIGINT       NOT NULL,              -- unit_price * quantity
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_order_menu (order_id, menu_id),
    KEY idx_order_item_menu (menu_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_menu  FOREIGN KEY (menu_id)  REFERENCES menu (id),
    CONSTRAINT ck_order_item_quantity CHECK (quantity BETWEEN 1 AND 20),
    CONSTRAINT ck_order_item_amount CHECK (line_amount = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
