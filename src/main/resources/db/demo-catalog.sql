SET client_encoding = 'UTF8'; -- nếu không, psql trên Windows sẽ đọc file UTF-8 này như WIN1252
-- Cửa hàng demo cho database Docker mới tạo. Docker chạy file này sau schema.sql.
-- Admin demo là admin@limeandlemon.local, mật khẩu MacMocAdmin2026.

INSERT INTO categories(name,slug) VALUES
    ('Áo thun','ao-thun'),('Áo sơ mi','ao-so-mi'),('Quần','quan'),('Váy & chân váy','vay-chan-vay'),('Áo khoác','ao-khoac');

INSERT INTO products(category_id,name,description,color,price,image_url,active,featured)
SELECT c.category_id,d.name,d.description,d.color,d.price,d.image_url,TRUE,d.featured
FROM (VALUES
    ('ao-thun','Áo thun oversize kem','Áo thun màu kem có thân rộng và vai trễ.','Be',229000.00,'/assets/products/featured/demo-tee-oversize-cream-featured-20261007.webp',TRUE),
    ('ao-thun','Áo thun cổ V xanh rêu','Áo thun xanh rêu cổ V, phom regular và tay ngắn.','Xanh rêu',219000.00,'/assets/products/featured/demo-tee-vneck-moss-featured-20261007.webp',TRUE),
    ('ao-thun','Áo thun tay dài trắng','Áo thun trắng cổ tròn với tay dài và phom regular.','Trắng',259000.00,'/assets/products/home/ao-thun-tay-dai-trang-20261007.webp',FALSE),
    ('ao-thun','Áo polo xanh navy','Áo polo xanh navy tay ngắn với cổ bẻ và nẹp ba nút.','Xanh navy',289000.00,'/assets/products/home/ao-polo-xanh-navy-20261007.webp',FALSE),
    ('ao-so-mi','Áo sơ mi Oxford xanh nhạt','Áo sơ mi xanh nhạt tay dài, cổ button-down và túi ngực.','Xanh nhạt',349000.00,'/assets/products/featured/demo-shirt-oxford-lblue-featured-20261007.webp',TRUE),
    ('ao-so-mi','Áo sơ mi cổ trụ be','Áo sơ mi be tay dài với cổ trụ ngắn và hàng nút trước.','Be',329000.00,'/assets/products/featured/demo-shirt-band-beige-featured-20261007.webp',TRUE),
    ('ao-so-mi','Áo sơ mi tay ngắn trắng','Áo sơ mi trắng tay ngắn với cổ nhọn và hàng nút trước.','Trắng',299000.00,'/assets/products/home/ao-so-mi-tay-ngan-trang-20261007.webp',FALSE),
    ('ao-so-mi','Áo sơ mi hai túi xanh rêu','Áo sơ mi khoác xanh rêu tay dài với hai túi nắp trước.','Xanh rêu',369000.00,'/assets/products/home/ao-so-mi-hai-tui-xanh-reu-20261007.webp',FALSE),
    ('quan','Quần chinos be','Quần chinos be ống đứng với cạp, đỉa thắt lưng và túi chéo.','Be',399000.00,'/assets/products/featured/demo-pant-chino-beige-featured-20261007.webp',TRUE),
    ('quan','Quần jogger xám','Quần jogger xám có dây rút ở cạp và bo gấu.','Xám',349000.00,'/assets/products/home/quan-jogger-xam-20261007.webp',FALSE),
    ('quan','Quần ống rộng đen','Quần đen cạp cao với ống rộng suông rõ nét.','Đen',429000.00,'/assets/products/featured/demo-pant-wide-black-featured-20261007.webp',TRUE),
    ('quan','Quần short túi hộp xanh rêu','Quần short xanh rêu dài đến gối với hai túi hộp nắp lớn.','Xanh rêu',319000.00,'/assets/products/home/quan-short-tui-hop-xanh-reu-20261007.webp',FALSE),
    ('quan','Quần jeans ống đứng xanh denim','Quần jeans xanh denim năm túi với đường may nổi và ống đứng.','Xanh denim',499000.00,'/assets/products/home/quan-jeans-ong-dung-xanh-denim-20261007.webp',FALSE),
    ('quan','Quần jeans ống loe đen','Quần jeans đen năm túi, ôm phần trên và loe rõ từ gối.','Đen',519000.00,'/assets/products/home/quan-jeans-ong-loe-den-20261007.webp',FALSE),
    ('vay-chan-vay','Đầm midi cổ vuông be','Đầm midi be cổ vuông, dây vai bản rộng và chân váy chữ A.','Be',549000.00,'/assets/products/home/dam-midi-co-vuong-be-20261007.webp',FALSE),
    ('vay-chan-vay','Đầm sơ mi hồng phấn','Đầm sơ mi hồng phấn dài ngang gối với cổ bẻ, hàng nút và đai buộc.','Hồng phấn',579000.00,'/assets/products/home/dam-so-mi-hong-phan-20261007.webp',FALSE),
    ('vay-chan-vay','Chân váy xếp ly xanh navy','Chân váy midi xanh navy có cạp rõ và các nếp ly đều.','Xanh navy',389000.00,'/assets/products/featured/demo-skirt-pleat-navy-featured-20261007.webp',TRUE),
    ('ao-khoac','Áo hoodie xám','Áo hoodie xám có mũ, dây rút và túi kangaroo trước.','Xám',459000.00,'/assets/products/home/ao-hoodie-xam-20261007.webp',FALSE),
    ('ao-khoac','Áo khoác bomber xanh rêu','Áo bomber xanh rêu có khóa kéo, cổ, tay và gấu bo.','Xanh rêu',649000.00,'/assets/products/featured/demo-jacket-bomber-moss-featured-20261007.webp',TRUE),
    ('ao-thun','Áo thun cơ bản','Áo thun cotton dùng cho dữ liệu minh họa.','Đen',199000.00,'/assets/products/home/ao-thun-co-ban-den-20261007.webp',FALSE),
    ('ao-khoac','Áo khoác denim xanh','Áo khoác denim xanh có cổ nhọn, hàng nút và hai túi ngực nắp.','Xanh denim',599000.00,'/assets/products/home/ao-khoac-denim-xanh-20261007.webp',FALSE)
) AS d(category_slug,name,description,color,price,image_url,featured)
JOIN categories c ON c.slug=d.category_slug;

-- Mỗi sản phẩm đều có đủ size S, M và L.
INSERT INTO product_sizes(product_id,size,stock)
SELECT p.product_id,s.size,s.stock FROM products p CROSS JOIN (VALUES ('S',12,1),('M',16,2),('L',10,3)) AS s(size,stock,ord)
ORDER BY p.product_id,s.ord;

-- password_hash = SHA-256 (dạng hex) của mật khẩu demo (xem service/Passwords.java).
INSERT INTO users(full_name,email,password_hash,role)
VALUES ('Quản trị viên','admin@limeandlemon.local',
        'df36e3f68ad7f8629ba4573cba6a543c8b9edbf620294d0889b141bb7c8b0ed0','ADMIN');
