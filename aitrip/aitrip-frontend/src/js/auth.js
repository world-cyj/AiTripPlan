// ===== 认证 =====
function openLoginModal() {
  document.getElementById('login-modal')?.remove();
  const modal = document.createElement('div');
  modal.className = 'modal-overlay';
  modal.id = 'login-modal';
  modal.innerHTML = `
    <div class="modal" style="max-width:380px">
      <div class="modal-header">
        <h2 class="modal-title">🔑 登录 AiTrip</h2>
        <button class="modal-close" onclick="document.getElementById('login-modal').remove()">✕</button>
      </div>
      <div style="display:flex;flex-direction:column;gap:16px">
        <div class="form-group">
          <label class="form-label">手机号</label>
          <input id="login-phone" class="form-input" placeholder="13800000001" maxlength="11"
            onkeydown="if(event.key==='Enter')doLogin()"/>
        </div>
        <div class="form-group">
          <label class="form-label">验证码 <span style="color:var(--accent);font-size:.7rem">(测试固定: 123456)</span></label>
          <input id="login-code" class="form-input" placeholder="123456" maxlength="6"
            onkeydown="if(event.key==='Enter')doLogin()"/>
        </div>
        <button class="btn btn-primary btn-full" onclick="doLogin()">🚀 登录</button>
        <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:center">
          <button class="tag" style="cursor:pointer" onclick="quickLogin('13800000001')">测试账号1</button>
          <button class="tag" style="cursor:pointer" onclick="quickLogin('13800000002')">测试账号2</button>
          <button class="tag" style="cursor:pointer" onclick="quickLogin('13800000003')">测试账号3</button>
        </div>
        <p style="font-size:.72rem;color:var(--text-muted);text-align:center">验证码固定为 123456</p>
      </div>
    </div>`;
  modal.addEventListener('click', e => { if(e.target===modal) modal.remove(); });
  document.body.appendChild(modal);
  setTimeout(() => document.getElementById('login-phone')?.focus(), 100);
}

function quickLogin(phone) {
  const pi = document.getElementById('login-phone');
  const ci = document.getElementById('login-code');
  if (pi) pi.value = phone;
  if (ci) ci.value = '123456';
  doLogin();
}

async function doLogin() {
  const phone = document.getElementById('login-phone')?.value.trim();
  const code  = document.getElementById('login-code')?.value.trim() || '123456';
  if (!phone || phone.length < 11) { toast('请输入11位手机号', 'error'); return; }
  const btn = document.querySelector('#login-modal .btn-primary');
  if (btn) { btn.disabled=true; btn.innerHTML='<div class="spinner"></div> 登录中...'; }
  try {
    const r = await api('/api/user/login', { method:'POST', body: JSON.stringify({phone, code}) });
    console.log('[Login] raw response:', JSON.stringify(r));

    // 后端返回 data 可能是字符串或对象，统一处理
    let parsed = r.data;
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed); } catch {}
    }
    // 如果还是字符串（双重嵌套）
    if (typeof parsed === 'string') {
      try { parsed = JSON.parse(parsed); } catch {}
    }
    console.log('[Login] parsed user:', parsed);

    if (!parsed || !parsed.token) {
      toast('登录失败：返回数据异常，请检查控制台', 'error');
      if (btn) { btn.disabled=false; btn.textContent='🚀 登录'; }
      return;
    }

    state.user = {
      userId:   parsed.userId,
      nickName: parsed.nickName || '用户_' + parsed.userId,
      token:    parsed.token,
      level:    parsed.level || 1
    };
    localStorage.setItem('aitrip_user', JSON.stringify(state.user));
    updateUserUI();
    document.getElementById('login-modal')?.remove();
    toast(`欢迎回来，${state.user.nickName} 👋`, 'success');
    console.log('[Login] state.user:', state.user);
    if (state.page === 'orders') loadOrders();
  } catch(e) {
    console.error('[Login] error:', e);
    toast('登录失败：' + e.message, 'error');
    if (btn) { btn.disabled=false; btn.textContent='🚀 登录'; }
  }
}

function doLogout() {
  if (state.user?.token) {
    api('/api/user/logout', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + state.user.token }
    }).catch(()=>{});
  }
  state.user = null;
  localStorage.removeItem('aitrip_user');
  updateUserUI();
  toast('已退出登录', 'info');
}

function updateUserUI() {
  const badge = document.getElementById('user-badge');
  if (!badge) return;
  if (state.user) {
    badge.innerHTML = `<span class="user-dot"></span> ${state.user.nickName} <span style="font-size:.62rem;opacity:.55">▼</span>`;
    badge.title = `userId: ${state.user.userId} | 点击退出`;
    badge.onclick = () => { if(confirm(`退出登录？\n当前用户: ${state.user.nickName}`)) doLogout(); };
  } else {
    badge.innerHTML = `<span class="user-dot" style="background:var(--text-muted);box-shadow:none"></span> 登录`;
    badge.title = '点击登录';
    badge.onclick = openLoginModal;
  }
}

function requireLogin(cb) {
  if (!state.user) { toast('请先登录', 'error'); openLoginModal(); return; }
  cb();
}
