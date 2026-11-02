-- 초기 데이터 (web/js/config.js의 seed와 동일). 재실행해도 중복되지 않는다.
INSERT IGNORE INTO category (id, name) VALUES (1, '싱글오리진'), (2, '블렌드');

INSERT IGNORE INTO menu (id, category_id, name, price, description, available) VALUES
 (1, 1, 'Columbia Nariñó', 5000, '콜롬비아 나리뇨 원두를 담은 커피빈 패키지입니다.', TRUE),
 (2, 1, 'Brazil Serra Do Caparaó', 7000, '브라질 세하 두 카파라오 원두를 담은 커피빈 패키지입니다.', TRUE),
 (3, 1, 'Columbia Quindío (White Wine Extended Fermentation)', 9000, '콜롬비아 킨디오 원두를 담은 커피빈 패키지입니다.', TRUE),
 (4, 1, 'Ethiopia Sidamo', 11000, '에티오피아 시다모 원두를 담은 커피빈 패키지입니다.', TRUE);
