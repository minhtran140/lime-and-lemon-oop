-- Chạy một lần trên database demo đã có; chạy lại vẫn an toàn. Link ảnh tùy chỉnh được giữ nguyên.
-- Ngày 07/11/2026 một volume Docker bị sửa ngoài repo: image_url trỏ vào /assets/products/covers/*-20261107.webp,
-- thư mục chưa từng có trong mã nguồn nên ảnh sản phẩm không hiện. File này trả về đúng ảnh có trên đĩa.
UPDATE products p SET image_url=art.ok
FROM (VALUES
    ('/assets/products/covers/ao-thun-co-ban-den-20261107.webp','/assets/products/home/ao-thun-co-ban-den-20261007.webp'),
    ('/assets/products/covers/ao-polo-xanh-navy-20261107.webp','/assets/products/home/ao-polo-xanh-navy-20261007.webp'),
    ('/assets/products/covers/ao-thun-tay-dai-trang-20261107.webp','/assets/products/home/ao-thun-tay-dai-trang-20261007.webp'),
    ('/assets/products/covers/demo-tee-vneck-moss-20261107.webp','/assets/products/featured/demo-tee-vneck-moss-featured-20261007.webp'),
    ('/assets/products/covers/demo-tee-oversize-cream-20261107.webp','/assets/products/featured/demo-tee-oversize-cream-featured-20261007.webp'),
    ('/assets/products/covers/ao-so-mi-hai-tui-xanh-reu-20261107.webp','/assets/products/home/ao-so-mi-hai-tui-xanh-reu-20261007.webp'),
    ('/assets/products/covers/ao-so-mi-tay-ngan-trang-20261107.webp','/assets/products/home/ao-so-mi-tay-ngan-trang-20261007.webp'),
    ('/assets/products/covers/demo-shirt-band-beige-20261107.webp','/assets/products/featured/demo-shirt-band-beige-featured-20261007.webp'),
    ('/assets/products/covers/demo-shirt-oxford-lblue-20261107.webp','/assets/products/featured/demo-shirt-oxford-lblue-featured-20261007.webp'),
    ('/assets/products/covers/quan-jeans-ong-loe-den-20261107.webp','/assets/products/home/quan-jeans-ong-loe-den-20261007.webp'),
    ('/assets/products/covers/quan-jeans-ong-dung-xanh-denim-20261107.webp','/assets/products/home/quan-jeans-ong-dung-xanh-denim-20261007.webp'),
    ('/assets/products/covers/quan-short-tui-hop-xanh-reu-20261107.webp','/assets/products/home/quan-short-tui-hop-xanh-reu-20261007.webp'),
    ('/assets/products/covers/demo-pant-wide-black-20261107.webp','/assets/products/featured/demo-pant-wide-black-featured-20261007.webp'),
    ('/assets/products/covers/quan-jogger-xam-20261107.webp','/assets/products/home/quan-jogger-xam-20261007.webp'),
    ('/assets/products/covers/demo-pant-chino-beige-20261107.webp','/assets/products/featured/demo-pant-chino-beige-featured-20261007.webp'),
    ('/assets/products/covers/demo-skirt-pleat-navy-20261107.webp','/assets/products/featured/demo-skirt-pleat-navy-featured-20261007.webp'),
    ('/assets/products/covers/dam-so-mi-hong-phan-20261107.webp','/assets/products/home/dam-so-mi-hong-phan-20261007.webp'),
    ('/assets/products/covers/dam-midi-co-vuong-be-20261107.webp','/assets/products/home/dam-midi-co-vuong-be-20261007.webp'),
    ('/assets/products/covers/ao-khoac-denim-xanh-20261107.webp','/assets/products/home/ao-khoac-denim-xanh-20261007.webp'),
    ('/assets/products/covers/demo-jacket-bomber-moss-20261107.webp','/assets/products/featured/demo-jacket-bomber-moss-featured-20261007.webp'),
    ('/assets/products/covers/ao-hoodie-xam-20261107.webp','/assets/products/home/ao-hoodie-xam-20261007.webp')
) AS art(bad,ok)
WHERE p.image_url=art.bad;
