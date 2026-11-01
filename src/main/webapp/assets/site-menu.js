// Các khung đóng/mở ở header: menu (điện thoại) và popover tìm kiếm (điện thoại và máy tính bảng).
// Escape đóng khung đang mở và trả focus về nút bật/tắt của nó; rời khỏi khung cũng đóng khung; mỗi lần chỉ mở một khung.
(() => {
    const menu = document.querySelector('.site-menu');
    if (!menu || !window.matchMedia) return;
    const search = document.querySelector('.mobile-search');

    // Giữ độ rộng này khớp với breakpoint 768px trong store.css.
    const compact = window.matchMedia('(max-width: 768px)');
    // Menu chỉ là một khung đóng/mở trên điện thoại; từ 769px trở lên nó là thanh nav luôn mở.
    const isPanel = (panel) => panel === search || compact.matches;
    const toggleOf = (panel) => panel.querySelector(':scope > summary');
    const panels = [menu, search].filter(Boolean);

    const close = (panel, returnFocus) => {
        if (!panel.open || !isPanel(panel)) return;
        panel.open = false;
        if (returnFocus) toggleOf(panel).focus();
    };

    menu.open = !compact.matches;
    compact.addEventListener('change', () => {
        menu.open = !compact.matches;
    });

    panels.forEach((panel) => {
        panel.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && panel.open && isPanel(panel)) {
                event.preventDefault();
                close(panel, true);
            }
        });
        // Nhấn Tab ra khỏi khung đang mở thì đóng khung, để focus không bị bỏ lại trên một control mà khung đang che.
        panel.addEventListener('focusout', (event) => {
            if (event.relatedTarget && !panel.contains(event.relatedTarget)) close(panel, false);
        });
        panel.addEventListener('toggle', () => {
            if (!panel.open || !isPanel(panel)) return;
            panels.forEach((other) => { if (other !== panel) close(other, false); });
            if (panel === search) search.querySelector('input')?.focus();
        });
    });

    // Chạm hoặc click ra ngoài các khung thì đóng chúng lại.
    document.addEventListener('click', (event) => {
        panels.forEach((panel) => { if (!panel.contains(event.target)) close(panel, false); });
    });
})();
