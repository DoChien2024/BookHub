const API = '/api/books';
const $  = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

let books = [];
let selected = new Set();

/*  Helpers  */
function canWrite() {
    return !!(window.__auth && window.__auth.authenticated);
}

function escapeHtml(s){
    return String(s ?? '')
        .replaceAll('&','&amp;')
        .replaceAll('<','&lt;')
        .replaceAll('>','&gt;');
}

function toast(msg, ok=true){
    const container = $('#toastContainer') || (() => {
        const d = document.createElement('div');
        d.id = 'toastContainer';
        document.body.appendChild(d);
        return d;
    })();
    const el = document.createElement('div');
    el.className = 'toast ' + (ok ? 'ok' : 'err');
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(() => el.remove(), 2500);
}

/* ===== Render table ===== */
function render(){
    const q = ($('#search')?.value || '').trim().toLowerCase();
    const data = q
        ? books.filter(b =>
            (b.title || '').toLowerCase().includes(q) ||
            (b.author || '').toLowerCase().includes(q) ||
            (b.category || '').toLowerCase().includes(q)
        )
        : books;

    const tbody = $('#tbody');
    if (!tbody) return;

    if (data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="muted">Không tìm thấy kết quả</td></tr>`;
        return;
    }

    const writable = canWrite();

    tbody.innerHTML = data.map(b => {
        const total = Number.isFinite(b.totalCopies) ? b.totalCopies : 1;
        const avail = Number.isFinite(b.availableCopies) ? b.availableCopies : Math.min(1, total);
        const outOfStock = avail <= 0;

        const stock = `<span class="stock-badge">${avail}/${total}</span>`;

        const borrowBtn = outOfStock
            ? `<div class="chip ghost" aria-disabled="true" title="Hết sách">Hết</div>`
            : `<div class="chip" onclick="openBorrowModal(${b.id}, '${escapeHtml(b.title)}')">Mượn</div>`;

        return `
        <tr>
          <td ${writable ? '' : 'class="hidden"'}><input type="checkbox" class="rowchk" data-id="${b.id}" ${selected.has(String(b.id)) ? 'checked' : ''}></td>
          <td>${escapeHtml(b.title)}</td>
          <td>${escapeHtml(b.author)}</td>
          <td>${escapeHtml(b.category)}</td>
          <td>${stock}</td>
          <td data-col="actions" ${writable ? '' : 'class="hidden"'}>
            ${borrowBtn}
            <div class="chip ghost" data-act="edit" data-id="${b.id}">Sửa</div>
            <div class="chip danger" data-act="del" data-id="${b.id}">Xóa</div>
          </td>
        </tr>`;
    }).join('');

    // checkbox hàng
    $$('.rowchk').forEach(chk => chk.onchange = e => {
        const id = e.target.dataset.id;
        e.target.checked ? selected.add(id) : selected.delete(id);
        const delBtn = $('#deleteSelectedBtn');
        if (delBtn) delBtn.disabled = selected.size === 0;
    });

    // trạng thái "chọn tất cả"
    if ($('#checkAll')) {
        $('#checkAll').checked = data.length > 0 && data.every(b => selected.has(String(b.id)));
    }
}

/* ===== Load data ===== */
async function load(){
    const tbody = $('#tbody');
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="6" class="muted">Đang tải dữ liệu...</td></tr>`;
    }

    try {
        const res = await fetch(API, { credentials: 'include' });
        books = await res.json();
        render();
    } catch {
        toast('Lỗi tải dữ liệu', false);
    }
}

/*  Create & Update (prompt-based)  */
function requireLogin() {
    if (!canWrite()) {
        toast('Vui lòng đăng nhập để thao tác', false);
        setTimeout(() => location.href = '/login', 400);
        return false;
    }
    return true;
}

function addBookPrompt(){
    if (!requireLogin()) return;

    const title = prompt('Tiêu đề:');   if (!title)   return;
    const author = prompt('Tác giả:');  if (author===null) return;
    const category = prompt('Thể loại:'); if (category===null) return;

    fetch(API, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({title, author, category}),
        credentials: 'include'
    })
        .then(r => { if (!r.ok) throw 0; return r.json(); })
        .then(() => { toast('Đã thêm sách'); load(); })
        .catch(() => toast('Thêm thất bại', false));
}

function editBookPrompt(id){
    if (!requireLogin()) return;
    const b = books.find(x => String(x.id) === String(id));
    if (!b) return;

    const title = prompt('Tiêu đề:', b.title);       if (title === null) return;
    const author = prompt('Tác giả:', b.author);     if (author === null) return;
    const category = prompt('Thể loại:', b.category);if (category === null) return;

    fetch(`${API}/${id}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({title, author, category}),
        credentials: 'include'
    })
        .then(r => { if (!r.ok) throw 0; return r.json(); })
        .then(() => { toast('Đã cập nhật'); load(); })
        .catch(() => toast('Cập nhật thất bại', false));
}

/*  Row actions (giữ Sửa/Xóa, thêm Mượn)  */
$('#tbody')?.addEventListener('click', e => {
    const btn = e.target.closest('[data-act]');
    if (!btn) return;
    const id = btn.dataset.id;

    if (btn.dataset.act === 'del') {
        if (!requireLogin()) return;
        if (!confirm('Xóa sách này?')) return;
        fetch(`${API}/${id}`, { method: 'DELETE', credentials: 'include' })
            .then(r => { if (!r.ok) throw 0; toast('Đã xóa'); load(); })
            .catch(() => toast('Xóa thất bại', false));
    }

    if (btn.dataset.act === 'edit') {
        editBookPrompt(id);
    }

    if (btn.dataset.act === 'borrow') {
        if (!requireLogin()) return;
        if (btn.getAttribute('aria-disabled') === 'true') return; // hết sách
        btn.setAttribute('aria-busy','true');
        fetch('/api/loans', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ bookId: id, days: 14 }),
            credentials: 'include'
        })
            .then(r => r.json().then(d => ({ ok:r.ok, d })))
            .then(({ ok, d }) => {
                if (ok) { toast('Mượn thành công'); load(); }
                else    { toast(d?.error || 'Mượn thất bại', false); btn.removeAttribute('aria-busy'); }
            })
            .catch(() => { toast('Mượn thất bại', false); btn.removeAttribute('aria-busy'); });
    }
});

/*  Bulk & Export  */
$('#checkAll')?.addEventListener('change', e => {
    if (e.target.checked) books.forEach(b => selected.add(String(b.id)));
    else selected.clear();

    const delBtn = $('#deleteSelectedBtn');
    if (delBtn) delBtn.disabled = selected.size === 0;
    render();
});

$('#deleteSelectedBtn')?.addEventListener('click', async () => {
    if (!requireLogin()) return;
    if (!selected.size) return;
    if (!confirm(`Xóa ${selected.size} mục đã chọn?`)) return;

    try {
        for (const id of [...selected]) {
            const r = await fetch(`${API}/${id}`, { method: 'DELETE', credentials: 'include' });
            if (!r.ok) throw 0;
        }
        selected.clear();
        toast('Đã xóa các mục');
        load();
    } catch {
        toast('Xóa hàng loạt thất bại', false);
    }
});

function exportCsv() {
    const rows = [['id','title','author','category','availableCopies','totalCopies']]
        .concat(books.map(b => [
            b.id, b.title, b.author, b.category,
            (Number.isFinite(b.availableCopies) ? b.availableCopies : ''),
            (Number.isFinite(b.totalCopies) ? b.totalCopies : '')
        ]));
    const csv = rows.map(r => r.map(v => `"${String(v ?? '').replaceAll('"','""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type:'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'books.csv';
    a.click();
    URL.revokeObjectURL(a.href);
}

/*  Expose  */
window.render = render;
window.load = load;
window.addBookPrompt = addBookPrompt;
window.editBookPrompt = editBookPrompt;
window.exportCsv = exportCsv;

/* ====== Popup mượn sách mở rộng ====== */
function openBorrowModal(bookId, bookTitle) {
    if (document.getElementById('borrowModal')) return;
    const html = `
    <div id="borrowModal" class="modal-overlay">
      <div class="modal-card">
          <h3 style="margin-top:0">Mượn sách: ${escapeHtml(bookTitle)}</h3>
          <label style="margin-top:16px">
              Họ và tên
              <input type="text" id="borrowFullName" placeholder="Nhập họ tên" autocomplete="off" required>
          </label>
          <label>
              Email
              <input type="email" id="borrowEmail" placeholder="Nhập email" autocomplete="off" required>
          </label>
          <label>
              Địa chỉ
              <input type="text" id="borrowAddress" placeholder="Nhập địa chỉ" autocomplete="off" required>
          </label>
          <label>
              Thời hạn trả (ngày)
              <input type="number" id="borrowDays" min="1" max="90" value="7" required>
          </label>
          <div style="margin-top:20px;display:flex;justify-content:flex-end;gap:14px;">
              <button class="chip" onclick="closeBorrowModal()">Hủy</button>
              <button class="chip primary" onclick="submitBorrow(${bookId})">Xác nhận mượn</button>
          </div>
      </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', html);
}
function closeBorrowModal() {
    const modal = document.getElementById('borrowModal');
    if (modal) modal.remove();
}

/*  Hiển thị Phiếu mượn sau khi mượn thành công  */
function showBorrowReceipt(loan) {
    const id         = loan?.id ?? '—';
    const title      = loan?.book?.title ?? loan?.bookTitle ?? '—';
    const borrower   = loan?.fullName ?? loan?.borrower?.fullName ?? '—';
    const email      = loan?.email ?? loan?.borrower?.email ?? '—';
    const address    = loan?.address ?? loan?.borrower?.address ?? '—';
    const borrowedAt = loan?.borrowedAt ? new Date(loan.borrowedAt) : new Date();
    const dueAt      = loan?.dueAt ? new Date(loan.dueAt) : null;
    const status     = loan?.status ?? 'IN_PROGRESS';

    document.getElementById('borrowModal')?.remove();
    document.getElementById('receiptModal')?.remove();

    const html = `
    <div id="receiptModal" class="modal-overlay">
      <div class="modal-card">
        <h3 style="margin-top:0">Phiếu mượn #${id}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <div><label>Sách</label><div><strong>${escapeHtml(title)}</strong></div></div>
          <div><label>Trạng thái</label><div>${escapeHtml(status)}</div></div>
          <div><label>Người mượn</label><div>${escapeHtml(borrower)}</div></div>
          <div><label>Email</label><div>${escapeHtml(email)}</div></div>
          <div><label>Địa chỉ</label><div>${escapeHtml(address)}</div></div>
          <div><label>Mượn lúc</label><div>${borrowedAt.toLocaleString()}</div></div>
          <div><label>Hạn trả</label><div>${dueAt ? dueAt.toLocaleString() : '—'}</div></div>
        </div>
        <div class="chips" style="margin-top:18px; justify-content:flex-end">
          <a class="chip ghost" href="/loans">Xem tất cả phiếu mượn</a>
          <button class="chip" id="printReceiptBtn" type="button">In phiếu</button>
          <button class="chip primary" id="closeReceiptBtn" type="button">Đóng</button>
        </div>
      </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', html);

    document.getElementById('closeReceiptBtn')?.addEventListener('click', () =>
        document.getElementById('receiptModal')?.remove()
    );
    document.getElementById('printReceiptBtn')?.addEventListener('click', () => {
        const w = window.open('', '_blank');
        w.document.write(`<html><head><title>Phiếu mượn #${id}</title>`);
        w.document.write(`<link rel="stylesheet" href="/css/argon-lite.css">`);
        w.document.write(`</head><body class="shell">${document.querySelector('#receiptModal .modal-card').outerHTML}</body></html>`);
        w.document.close();
        w.focus();
        w.print();
    });
}

/*  Gửi yêu cầu mượn  */
async function submitBorrow(bookId) {
    const btn = document.querySelector('#borrowModal .chip.primary');
    const fullName = document.getElementById('borrowFullName')?.value.trim();
    const email = document.getElementById('borrowEmail')?.value.trim();
    const address = document.getElementById('borrowAddress')?.value.trim();
    const days = parseInt(document.getElementById('borrowDays')?.value);
    if (!fullName || !email || !address || !days || days < 1) {
        toast('Nhập đủ và đúng thông tin!', false);
        return;
    }
    setBusy(btn, true);
    const res = await api('/api/loans', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({bookId, fullName, email, address, days})
    });
    if (res.ok) {
        toast('Đã gửi yêu cầu mượn sách!');
        showBorrowReceipt(res.data || {});
        if (typeof load === 'function') load(); // cập nhật tồn kho
    } else {
        toast(res.data?.error || 'Mượn sách thất bại', false);
        setBusy(btn, false);
    }
}