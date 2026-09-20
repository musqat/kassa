-- 테스트 용 시드 데이터
-- 카테고리 id 는 서브쿼리로 참조

INSERT INTO category (name, sort_order) VALUES
    ('음료', 1),
    ('간식', 2),
    ('생활용품', 3);

INSERT INTO product (category_id, name, price, stock, status) VALUES
    ((SELECT id FROM category WHERE name = '음료'), '생수 2L 6입', 4800, 120, 'ON_SALE'),
    ((SELECT id FROM category WHERE name = '음료'), '원두 콜드브루 1L', 12000, 40, 'ON_SALE'),
    ((SELECT id FROM category WHERE name = '음료'), '보리차 티백 100개입', 8900, 0, 'SOLD_OUT'),

    ((SELECT id FROM category WHERE name = '간식'), '다크 초콜릿 70%', 5600, 80, 'ON_SALE'),
    ((SELECT id FROM category WHERE name = '간식'), '견과 믹스 500g', 18900, 25, 'ON_SALE'),
    ((SELECT id FROM category WHERE name = '간식'), '수제 쿠키 선물세트', 32000, 10, 'ON_SALE'),

    ((SELECT id FROM category WHERE name = '생활용품'), '순면 수건 10장', 21000, 30, 'ON_SALE'),
    ((SELECT id FROM category WHERE name = '생활용품'), '스테인리스 텀블러 500ml', 26000, 15, 'ON_SALE'),

    -- 배송비 무료 기준(3만원) 위쪽 확인용
    ((SELECT id FROM category WHERE name = '생활용품'), '무선 주전자 1.7L', 45000, 12, 'ON_SALE'),

    -- 동시성 테스트용. 재고 1개에 주문이 몰리는 상황을 만든다
    ((SELECT id FROM category WHERE name = '생활용품'), '한정판 머그컵', 19000, 1, 'ON_SALE'),

    -- 목록 필터 확인용. 조회에 잡히면 안 된다
    ((SELECT id FROM category WHERE name = '간식'), '준비 중인 상품', 9900, 0, 'HIDDEN');
