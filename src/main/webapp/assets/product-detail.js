(() => {
    const root = document.querySelector('[data-pdp]');
    if (!root) return;

    // Chỉ một nơi giữ trạng thái: radio sizeId đang được chọn; nhãn size, tồn kho, số lượng và URL đều được tính lại từ nó.
    const sizes = [...root.querySelectorAll('.pdp-size input')];
    const sizeLabel = root.querySelector('[data-size-label]');
    const sizeError = root.querySelector('#pdp-size-error');
    const stock = root.querySelector('[data-stock-status]');
    const form = root.querySelector('[data-pdp-form]');
    const quantity = root.querySelector('input[name="quantity"]');
    const steps = [...root.querySelectorAll('[data-step]')];
    const add = root.querySelector('[data-add]');

    const selectedSize = () => sizes.find((input) => input.checked);
    // Ép số lượng thành số nguyên từ 1 đến max; ô trống hoặc không phải số thì thành 1.
    const clamp = (value, max) => Math.min(Math.max(1, Math.trunc(value) || 1), max);
    const remaining = (input) => Number(input.dataset.stock) - Number(input.dataset.inCart);

    // Nút − tắt khi số lượng <= 1, nút + tắt khi đạt max; cả hai tắt khi ô số lượng bị khóa.
    const syncSteps = () => {
        const value = Number(quantity.value);
        steps.forEach((button) => {
            button.disabled = quantity.disabled || (Number(button.dataset.step) < 0 ? value <= 1 : value >= Number(quantity.max));
        });
    };

    const render = () => {
        const selected = selectedSize();
        sizeLabel.textContent = selected ? selected.dataset.size : '';
        if (selected) sizeError.hidden = true;
        if (!form) return; // không có gì để mua: các size chỉ để xem thông tin
        stock.hidden = !sizeError.hidden; // mỗi lần chỉ hiện một thông báo

        const left = selected ? remaining(selected) : 0;
        const inCart = selected ? Number(selected.dataset.inCart) : 0;
        stock.classList.toggle('pdp-stock--empty', Boolean(selected && left <= 0));
        stock.classList.toggle('pdp-stock--hint', !selected);
        if (!selected) stock.textContent = 'Vui lòng chọn kích cỡ.';
        else if (left <= 0) stock.textContent = `Bạn đã có ${inCart} sản phẩm này trong giỏ, bằng số lượng còn lại.`;
        else stock.textContent = `Còn ${selected.dataset.stock} sản phẩm` + (inCart > 0 ? ` · Đã có ${inCart} trong giỏ` : '');

        quantity.disabled = !selected || left <= 0;
        if (!quantity.disabled) {
            quantity.max = left;
            quantity.value = clamp(Number(quantity.value), left);
        }
        // Chưa chọn size thì nút vẫn bật để khi bấm có thể báo cho khách biết còn thiếu gì.
        add.disabled = Boolean(selected && left <= 0);
        syncSteps();
    };

    // Dùng replaceState chứ không phải pushState: chọn size không phải là một lần xem trang mới, nên bấm Back một lần vẫn rời khỏi sản phẩm.
    const syncUrl = () => {
        const url = new URL(location.href);
        const selected = selectedSize();
        if (selected) url.searchParams.set('size', selected.dataset.size);
        else url.searchParams.delete('size');
        if (url.href !== location.href) history.replaceState(null, '', url);
    };

    sizes.forEach((input) => input.addEventListener('change', () => {
        render();
        syncUrl();
    }));

    if (quantity) {
        quantity.addEventListener('input', syncSteps);
        quantity.addEventListener('change', () => {
            quantity.value = clamp(Number(quantity.value), Number(quantity.max));
            syncSteps();
        });
        steps.forEach((button) => button.addEventListener('click', () => {
            quantity.value = clamp(Number(quantity.value) + Number(button.dataset.step), Number(quantity.max));
            syncSteps();
        }));
    }

    if (form) {
        form.addEventListener('submit', (event) => {
            if (!selectedSize()) {
                event.preventDefault();
                sizeError.hidden = false;
                stock.hidden = true;
                sizeError.setAttribute('role', 'alert');
                const first = sizes.find((input) => !input.disabled);
                if (first) first.focus();
                return;
            }
            // Trạng thái bận và việc chặn submit lần hai do cơ chế dùng chung trong form-messages.js đảm nhiệm.
        });
        // Cơ chế dùng chung đã hoàn lại trạng thái bận khi khôi phục từ bfcache; dòng này đọc lại tồn kho cho size đang hiển thị.
        window.addEventListener('pageshow', (event) => { if (event.persisted) render(); });
    }

    render();
})();
