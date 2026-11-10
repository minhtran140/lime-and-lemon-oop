-- Chạy một lần trên database demo đã có; chạy lại vẫn an toàn. Chỉ xóa câu ghi chú ở cuối mô tả, phần còn lại giữ nguyên.
SET client_encoding = 'UTF8';
UPDATE products
SET description=regexp_replace(description,'\s*Sản phẩm demo(; ảnh minh họa được tạo bằng AI)?\.$','')
WHERE description ~ '\s*Sản phẩm demo(; ảnh minh họa được tạo bằng AI)?\.$';
