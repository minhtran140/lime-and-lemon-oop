// Cho các khối của storefront hiện dần khi cuộn tới. Không làm gì nếu khách chọn giảm chuyển động (prefers-reduced-motion) hoặc ở trang quản trị.
// Thứ gì đã nằm trên màn hình lúc tải trang (tiêu đề trang, hàng đầu của lưới) luôn hiển thị, không bị ẩn trước.
(() => {
    if (!('IntersectionObserver' in window)
        || window.matchMedia('(prefers-reduced-motion: reduce)').matches
        || document.querySelector('main.admin-page')) return;

    const blocks = document.querySelectorAll(
        '.home-section-heading, .home-collection, .home-outfit, .product-card');
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            const block = entry.target;
            block.classList.add('is-visible');
            // Trả element về cho các transition hover của riêng nó khi hiệu ứng hiện dần đã xong.
            block.addEventListener('transitionend', () => block.classList.remove('reveal', 'is-visible'), { once: true });
            observer.unobserve(block);
        });
    }, { rootMargin: '0px 0px -40px 0px' });

    blocks.forEach((block, index) => {
        if (block.getBoundingClientRect().top < window.innerHeight) return;
        block.style.setProperty('--index', index % 4);
        block.classList.add('reveal');
        observer.observe(block);
    });
})();
