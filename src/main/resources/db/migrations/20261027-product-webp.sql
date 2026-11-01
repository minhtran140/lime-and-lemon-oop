-- Chạy một lần trên database demo đã có; chạy lại vẫn an toàn. Link ảnh tùy chỉnh được giữ nguyên.
UPDATE products p SET image_url=art.webp
FROM (VALUES
    ('/assets/products/ao-thun-tay-dai-trang-ai.png','/assets/products/home/ao-thun-tay-dai-trang-20261007.webp'),
    ('/assets/products/ao-polo-xanh-navy-ai.png','/assets/products/home/ao-polo-xanh-navy-20261007.webp'),
    ('/assets/products/ao-so-mi-tay-ngan-trang-ai.png','/assets/products/home/ao-so-mi-tay-ngan-trang-20261007.webp'),
    ('/assets/products/ao-so-mi-hai-tui-xanh-reu-ai.png','/assets/products/home/ao-so-mi-hai-tui-xanh-reu-20261007.webp'),
    ('/assets/products/quan-jogger-xam-ai.png','/assets/products/home/quan-jogger-xam-20261007.webp'),
    ('/assets/products/quan-short-tui-hop-xanh-reu-ai.png','/assets/products/home/quan-short-tui-hop-xanh-reu-20261007.webp'),
    ('/assets/products/quan-jeans-ong-dung-xanh-denim-ai.png','/assets/products/home/quan-jeans-ong-dung-xanh-denim-20261007.webp'),
    ('/assets/products/quan-jeans-ong-loe-den-ai.png','/assets/products/home/quan-jeans-ong-loe-den-20261007.webp'),
    ('/assets/products/dam-midi-co-vuong-be-ai.png','/assets/products/home/dam-midi-co-vuong-be-20261007.webp'),
    ('/assets/products/dam-so-mi-hong-phan-ai.png','/assets/products/home/dam-so-mi-hong-phan-20261007.webp'),
    ('/assets/products/ao-hoodie-xam-ai.png','/assets/products/home/ao-hoodie-xam-20261007.webp'),
    ('/assets/products/ao-thun-co-ban-den-ai.png','/assets/products/home/ao-thun-co-ban-den-20261007.webp'),
    ('/assets/products/ao-khoac-denim-xanh-ai.png','/assets/products/home/ao-khoac-denim-xanh-20261007.webp')
) AS art(png,webp)
WHERE p.image_url=art.png;
