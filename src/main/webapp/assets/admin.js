(() => {
    // Trả về mảng tất cả element khớp selector (querySelectorAll chỉ trả NodeList).
    const $$ = (s, r = document) => [...r.querySelectorAll(s)];

    // Theo dõi thay đổi: nút lưu bị vô hiệu hóa cho đến khi một control thuộc form của nó khác giá trị
    // server đã lưu. Không có JS thì mọi nút vẫn bật và server bỏ qua lần lưu không đổi gì.
    const changed = c => {
        if (c.type === 'checkbox') return c.checked !== c.defaultChecked;
        if (c.tagName === 'SELECT') return [...c.options].some(o => o.selected !== o.defaultSelected);
        return c.value !== c.defaultValue;
    };
    const saveOf = f => f.querySelector('[data-save]');
    // Dòng được render lại sau một lần lưu thất bại mang theo các chỉnh sửa mà server chưa lưu.
    const unsaved = f => !!f.closest('[data-unsaved]');
    $$('form [data-save]').forEach(b => { b.disabled = !unsaved(b.form); });
    $$('tr[data-unsaved]').forEach(tr => tr.setAttribute('data-dirty', ''));
    const refresh = f => {
        // Form có thay đổi nếu dòng còn chỉnh sửa chưa lưu, hoặc có ô nhập (trừ nút và ô ẩn) khác giá trị ban đầu.
        const dirty = unsaved(f) || [...f.elements].some(c => !/^(submit|button|hidden)$/.test(c.type) && changed(c));
        saveOf(f).disabled = !dirty;
        f.closest('tr')?.toggleAttribute('data-dirty', dirty);
    };
    const onInput = e => {
        const f = e.target.form;
        if (f && saveOf(f)) refresh(f);
    };
    document.addEventListener('input', onInput);
    document.addEventListener('change', onInput);
    let submitting = false;
    addEventListener('beforeunload', e => {
        if (!submitting && document.querySelector('tr[data-dirty]')) e.preventDefault();
    });
    addEventListener('pageshow', () => { submitting = false; });
    // Lưu một dòng sẽ tải lại trang và làm mất chỉnh sửa ở các dòng khác, nên hỏi xác nhận ngay trong event submit.
    // Nếu hỏi ở beforeunload mà khách bấm Hủy, form-messages.js đã đánh dấu form là đang bận và nó sẽ bị kẹt.
    // Hỏi ở đây thì bấm Hủy gọi preventDefault(); form-messages.js (nạp sau tệp này) thấy submit đã bị hủy nên bỏ qua.
    // Chống bấm gửi hai lần nằm ở form-messages.js.
    document.addEventListener('submit', e => {
        const row = e.target.closest('tr');
        if ($$('tr[data-dirty]').some(tr => tr !== row)
            && !confirm('Các dòng khác có thay đổi chưa lưu và sẽ mất khi trang tải lại. Vẫn lưu dòng này?')) {
            e.preventDefault();
            return;
        }
        submitting = true;
        row?.removeAttribute('data-dirty');
    });
})();
