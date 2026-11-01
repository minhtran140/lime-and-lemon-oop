// Hành vi form dùng chung cho storefront và các trang quản trị:
// 1. Thông báo kiểm tra dữ liệu có sẵn của trình duyệt được đổi sang tiếng Việt.
// 2. Một cơ chế chống bấm gửi hai lần (trạng thái bận) cho mọi form POST (xem bên dưới).
// 3. Focus vào khung tóm tắt lỗi hoặc thông báo lỗi đầu tiên (data-focus-error-summary) sau khi trang tải.
(() => {
    const MISSING = 'Vui lòng điền thông tin này.';
    // Ô bắt buộc chỉ toàn khoảng trắng: trình duyệt coi là đã điền, nên code tự đặt thông báo lỗi.
    const blank = (el) => el.required && /^(textarea|text|search|tel|url|email|password)$/.test(el.type) && el.value !== '' && !el.value.trim();
    const reset = (el) => {
        if (el.setCustomValidity) {
            el.setCustomValidity(blank(el) ? MISSING : '');
        }
    };
    const text = (el) => {
        const v = el.validity;
        if (v.valueMissing) return el.tagName === 'SELECT' ? 'Vui lòng chọn một mục.' : MISSING;
        if (v.typeMismatch) return el.type === 'url' ? 'Vui lòng nhập địa chỉ liên kết hợp lệ.' : 'Vui lòng nhập địa chỉ email hợp lệ.';
        if (v.tooShort) return `Vui lòng nhập ít nhất ${el.minLength} ký tự.`;
        if (v.tooLong) return `Vui lòng nhập tối đa ${el.maxLength} ký tự.`;
        if (v.rangeUnderflow) return `Giá trị phải lớn hơn hoặc bằng ${el.min}.`;
        if (v.rangeOverflow) return `Giá trị phải nhỏ hơn hoặc bằng ${el.max}.`;
        if (v.stepMismatch) return 'Giá trị không hợp lệ.';
        if (v.badInput) return 'Vui lòng nhập một số hợp lệ.';
        if (v.patternMismatch) return el.title || 'Vui lòng nhập đúng định dạng.';
        return '';
    };

    document.addEventListener('invalid', (e) => {
        reset(e.target);
        if (!blank(e.target)) e.target.setCustomValidity(text(e.target));
    }, true);
    ['input', 'change'].forEach((t) => document.addEventListener(t, (e) => reset(e.target), true));
    document.querySelectorAll('input, textarea').forEach(reset);

    // Chống bấm gửi hai lần. Mọi <form method="post"> đều được bảo vệ, không cần thêm markup:
    //   - lần submit đầu đặt aria-busy="true" cho nút gửi và chặn lần submit thứ hai;
    //   - data-busy-label (tùy chọn) trên nút đó đổi chữ hiển thị ("Đang đặt hàng…");
    //   - data-no-busy trên form để không áp dụng cơ chế này;
    //   - submit mà handler khác đã hủy (kiểm tra phía client thất bại, bước xác nhận "armed") thì bỏ qua;
    //   - trạng thái được hoàn lại khi trang quay về từ back/forward cache.
    // Cố ý không disable nút: nút gửi bị disable sẽ làm mất name/value của nó trong dữ liệu POST.
    const labelNode = (button) => [...button.childNodes].find((n) => n.nodeType === Node.TEXT_NODE && n.data.trim());
    document.addEventListener('submit', (e) => {
        const form = e.target;
        if (e.defaultPrevented || !(form instanceof HTMLFormElement)
            || form.method.toLowerCase() !== 'post' || form.hasAttribute('data-no-busy')) return;
        if (form.dataset.busy) {
            e.preventDefault();
            return;
        }
        const button = e.submitter || form.querySelector('button:not([type]), button[type="submit"]');
        if (!button) return;
        form.dataset.busy = 'true';
        button.setAttribute('aria-busy', 'true');
        const busyLabel = button.dataset.busyLabel;
        const node = busyLabel && labelNode(button);
        if (node) {
            button.dataset.idleLabel = node.data;
            node.data = busyLabel;
        }
    });
    window.addEventListener('pageshow', (e) => {
        if (!e.persisted) return;
        document.querySelectorAll('form[data-busy]').forEach((form) => delete form.dataset.busy);
        document.querySelectorAll('button[aria-busy="true"]').forEach((button) => {
            button.removeAttribute('aria-busy');
            if (button.dataset.idleLabel === undefined) return;
            const node = labelNode(button);
            if (node) node.data = button.dataset.idleLabel;
            delete button.dataset.idleLabel;
        });
    });

    document.querySelector('[data-focus-error-summary]')?.focus();
})();
