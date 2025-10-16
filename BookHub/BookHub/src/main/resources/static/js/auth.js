function toast(msg, ok = true) {
    const d = document.getElementById('toastContainer') || (() => {
        const x = document.createElement('div');
        x.id = 'toastContainer';
        document.body.appendChild(x);
        return x;
    })();
    const el = document.createElement('div');
    el.className = 'toast ' + (ok ? 'ok' : 'err');
    el.textContent = msg;
    d.appendChild(el);
    setTimeout(() => el.remove(), 2200);
}

/*  Helper chung cho fetch  */
async function api(url, options = {}) {
    const res = await fetch(url, {
        credentials: 'include',
        cache: 'no-store',
        ...options
    });
    let data = null;
    try { data = await res.clone().json(); } catch {}
    return { ok: res.ok, status: res.status, data, raw: res };
}

/*  Utils nhỏ  */
function normalizeRole(role) {
    // "ROLE_ADMIN" -> "ADMIN"
    return String(role || '').replace(/^ROLE_/, '') || 'USER';
}
function setBusy(btn, busy, labelBusy = 'Đang xử lý...') {
    if (!btn) return;
    if (busy) {
        btn.dataset._label = btn.textContent;
        btn.disabled = true;
        btn.textContent = labelBusy;
    } else {
        btn.disabled = false;
        if (btn.dataset._label) btn.textContent = btn.dataset._label;
        delete btn.dataset._label;
    }
}

/* Auth API wrappers */
async function register() {
    const btn = document.getElementById('registerBtn');
    setBusy(btn, true, 'Đang đăng ký...');

    const username = document.getElementById('r-username')?.value?.trim();
    const email    = document.getElementById('r-email')?.value?.trim();
    const password = document.getElementById('r-password')?.value;

    if (!username || !email || !password) {
        toast('Vui lòng nhập đủ thông tin', false);
        return setBusy(btn, false);
    }

    const { ok, data } = await api('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
    });

    if (ok) {
        toast('Đăng ký thành công');
        location.href = '/login';
    } else {
        toast(data?.error || `Đăng ký thất bại (${data?.status || ''})`, false);
        setBusy(btn, false);
    }
}

async function login() {
    const btn = document.getElementById('loginBtn');
    setBusy(btn, true, 'Đang đăng nhập...');

    const username = document.getElementById('l-username')?.value?.trim();
    const password = document.getElementById('l-password')?.value;

    if (!username || !password) {
        toast('Nhập tên đăng nhập & mật khẩu', false);
        return setBusy(btn, false);
    }

    const { ok, data } = await api('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    if (ok) {
        toast('Đăng nhập thành công');
        const back = sessionStorage.getItem('afterLogin') || '/';
        sessionStorage.removeItem('afterLogin');
        location.href = back;
    } else {
        toast(data?.error || 'Sai thông tin đăng nhập', false);
        setBusy(btn, false);
    }
}

async function logout() {
    const { ok } = await api('/api/auth/logout', { method: 'POST' });
    if (ok) {
        toast('Đã đăng xuất');
        setTimeout(() => location.reload(), 400);
    } else {
        toast('Đăng xuất lỗi', false);
    }
}

async function me() {
    try {
        const { ok, data } = await api('/api/auth/me');
        return ok ? data : { authenticated: false };
    } catch {
        return { authenticated: false };
    }
}

/*  Render vùng auth (header)  */
async function renderAuthArea() {
    const area = document.getElementById('authArea');
    if (!area) return;

    const info = await me();

    if (info?.authenticated) {
        const role = normalizeRole(info.role);
        area.innerHTML = `
      <div class="chip ghost">Xin chào, ${info.username} (${role})</div>
      <button class="chip danger" id="logoutBtn" type="button">Đăng xuất</button>
    `;
        document.getElementById('logoutBtn')?.addEventListener('click', logout);
    } else {
        area.innerHTML = `
      <a class="chip ghost" href="/login" id="goLogin">Đăng nhập</a>
      <a class="chip" href="/register">Đăng ký</a>
    `;
        document.getElementById('goLogin')?.addEventListener('click', () => {
            sessionStorage.setItem('afterLogin', location.pathname + location.search);
        });
    }
}

/*  Auto init  */
document.addEventListener('DOMContentLoaded', () => {
    renderAuthArea();
    window.register = register;
    window.login = login;
    window.logout = logout;
    window.me = me;
    window.renderAuthArea = renderAuthArea;
});