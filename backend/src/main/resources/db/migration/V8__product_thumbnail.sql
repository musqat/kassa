-- 시드 상품에 사진을 붙인다. 파일은 프런트 public/products 에 있다
-- 언스플래시 라이선스, 출처는 public/products/CREDITS.md

UPDATE product SET thumbnail_url = '/products/water.jpg' WHERE name = '생수 2L 6입';
UPDATE product SET thumbnail_url = '/products/cold-brew.jpg' WHERE name = '원두 콜드브루 1L';
UPDATE product SET thumbnail_url = '/products/barley-tea.jpg' WHERE name = '보리차 티백 100개입';
UPDATE product SET thumbnail_url = '/products/chocolate.jpg' WHERE name = '다크 초콜릿 70%';
UPDATE product SET thumbnail_url = '/products/nuts.jpg' WHERE name = '견과 믹스 500g';
UPDATE product SET thumbnail_url = '/products/cookies.jpg' WHERE name = '수제 쿠키 선물세트';
UPDATE product SET thumbnail_url = '/products/towels.jpg' WHERE name = '순면 수건 10장';
UPDATE product SET thumbnail_url = '/products/tumbler.jpg' WHERE name = '스테인리스 텀블러 500ml';
UPDATE product SET thumbnail_url = '/products/kettle.jpg' WHERE name = '무선 주전자 1.7L';
UPDATE product SET thumbnail_url = '/products/mug.jpg' WHERE name = '한정판 머그컵';
