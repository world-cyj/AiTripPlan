// ===== 配置（可在页面顶部修改端口）=====
const CONFIG = {
  backendPort: 8080,   // ← 改这里修改后端端口（默认8080）
  agentPort:   8081,   // ← 改这里修改Agent端口
};

const API = {
  get backend() { return `http://localhost:${CONFIG.backendPort}`; },
  get agent()   { return `http://localhost:${CONFIG.agentPort}`; },
};

// ===== 全局状态 =====
const state = {
  user: null,
  page: 'home',
  currentConvId: null,
};

// ===== 工具函数 =====
const $ = (s, ctx = document) => ctx.querySelector(s);
const $$ = (s, ctx = document) => [...ctx.querySelectorAll(s)];

// 解析后端返回的 data 字段（可能是字符串或对象）
function parseData(r) {
  if (!r) return null;
  const d = r.data;
  if (d === null || d === undefined) return null;
  if (typeof d === 'string') {
    try { return JSON.parse(d); } catch { return d; }
  }
  return d;
}

async function api(path, opts = {}) {
  const base = path.startsWith('/api/agent') ? API.agent : API.backend;
  const headers = { 'Content-Type': 'application/json', ...opts.headers };
  if (state.user?.token) headers['Authorization'] = 'Bearer ' + state.user.token;
  const r = await fetch(base + path, { ...opts, headers });
  if (!r.ok) throw new Error('HTTP ' + r.status);
  return await r.json();
}

function toast(msg, type = 'info') {
  const icons = { success: '✓', error: '✕', info: '●' };
  const color = type==='success'?'teal':type==='error'?'rose':'accent';
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span style="color:var(--${color})">${icons[type]}</span> <span>${msg}</span>`;
  $('#toast-container').appendChild(el);
  setTimeout(() => { el.classList.add('removing'); setTimeout(()=>el.remove(),280); }, 3200);
}

function showPage(id) {
  $$('.page').forEach(p => p.classList.remove('active'));
  $$('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.page === id));
  const pg = $('#page-' + id);
  if (pg) { pg.classList.add('active'); state.page = id; }
  window.scrollTo(0, 0);
  const init = { home:initHome, plan:initPlan, explore:initExplore, seckill:initSeckill, orders:initOrders };
  init[id]?.();
}

const THUMBS = ['🏯','🗼','🌅','🏔','🌸','⛩','🏝','🎭','🌿','🦁','🗿','🌊','🏟','🎪','🌄'];
const BG_COLORS = ['#1a2a1a','#1a1a2a','#2a1a1a','#1a2a2a','#2a2a1a','#1a1e2a','#2a1a22'];
const randThumb = id => THUMBS[(id||0) % THUMBS.length];
const randBg    = id => BG_COLORS[(id||0) % BG_COLORS.length];

function statusBadge(s) {
  const m={1:'⏳ 待付款',2:'✓ 已付款',3:'🎫 已核销',4:'✕ 已取消',5:'↩ 退款中',6:'✓ 已退款'};
  const c={1:'badge-gold',2:'badge-teal',3:'badge-teal',4:'badge-rose',5:'badge-rose',6:'badge-teal'};
  return `<span class="badge ${c[s]||'badge-gold'}">${m[s]||'未知'}</span>`;
}

function attractionCard(a) {
  const id = a.id || a.attractionId || 0;
  return `<div class="attraction-card" onclick="openAttractionDetail(${id})">
    <div class="attraction-thumb" style="background:${randBg(id)}">${randThumb(id)}</div>
    <div class="attraction-body">
      <div class="attraction-name">${a.name||'未知'}</div>
      <div class="attraction-city">📍 ${a.city||''}</div>
      <div class="attraction-meta">
        <span class="attraction-price">¥${a.price||0}<small>/人</small></span>
        <span class="attraction-score">★ ${a.score||'—'}</span>
      </div>
    </div>
  </div>`;
}

// 端口设置面板
function openPortSettings() {
  const existing = document.getElementById('port-modal');
  if (existing) { existing.remove(); return; }
  const modal = document.createElement('div');
  modal.id = 'port-modal';
  modal.className = 'modal-overlay';
  modal.innerHTML = `
    <div class="modal" style="max-width:360px">
      <div class="modal-header">
        <h2 class="modal-title">⚙️ 端口设置</h2>
        <button class="modal-close" onclick="document.getElementById('port-modal').remove()">✕</button>
      </div>
      <div style="display:flex;flex-direction:column;gap:14px">
        <div class="form-group">
          <label class="form-label">后端端口（aitrip-backend）</label>
          <input id="port-backend" class="form-input" value="${CONFIG.backendPort}" placeholder="8080"/>
        </div>
        <div class="form-group">
          <label class="form-label">Agent 端口（aitrip-agent）</label>
          <input id="port-agent" class="form-input" value="${CONFIG.agentPort}" placeholder="8081"/>
        </div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-ghost btn-sm" style="flex:1" onclick="testConnection()">🔌 测试连接</button>
          <button class="btn btn-primary btn-sm" style="flex:1" onclick="applyPortSettings()">✓ 应用</button>
        </div>
        <div id="conn-test-result" style="font-size:.78rem;min-height:1.2em"></div>
        <div style="background:rgba(251,191,36,.06);border:1px solid rgba(251,191,36,.15);border-radius:8px;padding:12px;font-size:.76rem;color:var(--text-muted);line-height:1.7">
          <strong style="color:var(--accent)">📋 启动命令</strong><br>
          在 IDEA 中运行 <code style="color:var(--teal)">AiTripBackendApplication</code><br>
          或在终端执行：<br>
          <code style="color:var(--teal)">mvn spring-boot:run -pl aitrip-backend</code><br><br>
          当前配置: backend=<code>${CONFIG.backendPort}</code> agent=<code>${CONFIG.agentPort}</code>
        </div>
      </div>
    </div>`;
  modal.addEventListener('click', e => { if(e.target===modal) modal.remove(); });
  document.body.appendChild(modal);
}

function applyPortSettings() {
  const bp = parseInt($('#port-backend')?.value) || 8080;
  const ap = parseInt($('#port-agent')?.value)   || 8081;
  CONFIG.backendPort = bp;
  CONFIG.agentPort   = ap;
  localStorage.setItem('aitrip_ports', JSON.stringify({backendPort:bp,agentPort:ap}));
  document.getElementById('port-modal')?.remove();
  toast(`端口已更新：backend=${bp}, agent=${ap}`, 'success');
  const init = { home:initHome, plan:initPlan, explore:initExplore, seckill:initSeckill, orders:initOrders };
  init[state.page]?.();
}

async function testConnection() {
  const bp = parseInt($('#port-backend')?.value) || CONFIG.backendPort;
  const resultEl = $('#conn-test-result');
  if (resultEl) resultEl.innerHTML = '<span style="color:var(--text-muted)">测试中...</span>';
  try {
    const r = await fetch(`http://localhost:${bp}/actuator/health`, {signal: AbortSignal.timeout(4000)});
    const data = await r.json();
    const ok = data.status === 'UP';
    if (resultEl) resultEl.innerHTML = ok
      ? `<span style="color:var(--teal)">✓ 连接成功 (status: ${data.status})</span>`
      : `<span style="color:var(--rose)">✗ 服务异常 (status: ${data.status})</span>`;
  } catch(e) {
    if (resultEl) resultEl.innerHTML = `<span style="color:var(--rose)">✗ 无法连接端口 ${bp}：${e.message}</span>`;
  }
}
