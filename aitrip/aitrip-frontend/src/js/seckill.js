// ===== 秒杀页 =====
// 预检状态：true=有资格，false=无资格，null=未检查
let preCheckPassed = null;

async function initSeckill() {
  preCheckPassed = null;
  const box = $('#seckill-vouchers');
  if (!box) return;
  box.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';
  try {
    const r = await api('/api/attractions/search?city=&keyword=');
    const list = parseData(r) || [];
    if (!list.length) {
      box.innerHTML = '<div class="empty-state"><span class="icon">🎫</span><p>暂无秒杀门票</p></div>';
      return;
    }
    box.innerHTML = list.slice(0,10).map(a => `
      <div class="card" id="voucher-card-${a.id}"
           style="display:flex;align-items:center;gap:14px;padding:14px 16px;cursor:pointer;margin-bottom:10px;transition:border-color .2s"
           onclick="selectVoucher(${a.id},'${(a.name||'').replace(/'/g,'').replace(/}/g,'')}',${a.price||0})">
        <span style="font-size:1.8rem">${randThumb(a.id)}</span>
        <div style="flex:1;min-width:0">
          <div style="font-weight:600;font-size:.9rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${a.name}</div>
          <div style="font-size:.73rem;color:var(--text-muted);margin-top:3px">📍 ${a.city||''} · ¥${a.price||0}/人</div>
        </div>
        <span class="badge badge-teal">秒杀</span>
      </div>`).join('');
  } catch(e) {
    box.innerHTML = `<div class="empty-state"><span class="icon">⚠️</span><p>加载失败: ${e.message}</p></div>`;
  }
}

function selectVoucher(id, name, price) {
  if (!id) return;
  // 重置预检状态
  preCheckPassed = null;
  $('#seckill-voucher-id').value = id;
  const nm = $('#selected-voucher-name');
  if (nm) nm.innerHTML = `<span style="color:var(--text-primary);font-weight:600">${name}</span> <span style="color:var(--accent)">¥${price}/人</span> <span class="badge badge-gold" style="margin-left:4px">ID:${id}</span>`;
  // 清空上次预检和结果
  const pr = $('#precheck-result');
  const sr = $('#seckill-result');
  if (pr) pr.innerHTML = '';
  if (sr) sr.innerHTML = '';
  // 高亮选中卡片
  $$('#seckill-vouchers .card').forEach(c => c.style.borderColor = '');
  const card = document.getElementById(`voucher-card-${id}`);
  if (card) card.style.borderColor = 'var(--accent)';
  // 重置抢票按钮状态
  updateSeckillBtn();
  toast(`已选择：${name}`, 'info');
}

function updateSeckillBtn() {
  const btn = $('#seckill-btn');
  if (!btn) return;
  // 未预检：正常显示
  btn.disabled = false;
  btn.textContent = '🎫 立即抢票';
}

async function doPreCheck() {
  requireLogin(async () => {
    const voucherId = $('#seckill-voucher-id')?.value;
    if (!voucherId) { toast('请先在左侧选择景区', 'error'); return; }
    const box = $('#precheck-result');
    box.innerHTML = '<div style="display:flex;align-items:center;gap:8px;padding:10px 0;color:var(--text-muted);font-size:.84rem"><div class="spinner"></div> 预检中...</div>';
    try {
      const r = await api(`/api/seckill/pre-check?voucherId=${voucherId}&userId=${state.user.userId}`);
      const d = parseData(r);
      console.log('[PreCheck] result:', d);
      // 解析资格
      let eligible = true;
      let reason = '';
      if (typeof d === 'string') {
        try {
          const obj = JSON.parse(d);
          eligible = obj.eligible !== false;
          reason = obj.reason || obj.message || d;
        } catch {
          // 字符串直接判断
          eligible = !d.includes('售完') && !d.includes('无资格') && !d.includes('已抢购');
          reason = d;
        }
      } else if (typeof d === 'object' && d !== null) {
        eligible = d.eligible !== false;
        reason = d.reason || d.message || '';
      }
      preCheckPassed = eligible;
      box.innerHTML = `<div style="display:flex;align-items:center;gap:10px;padding:10px 0">
        <span style="font-size:1.5rem">${eligible?'✅':'❌'}</span>
        <div><div style="font-weight:600;font-size:.88rem">${eligible?'您有资格参与秒杀':'暂无资格'}</div>
        <div style="font-size:.76rem;color:var(--text-muted);margin-top:3px">${reason}</div></div></div>`;
    } catch(e) {
      preCheckPassed = null;
      box.innerHTML = `<p style="color:var(--rose);font-size:.83rem">预检失败: ${e.message}</p>`;
    }
  });
}

async function doSeckill() {
  requireLogin(async () => {
    const voucherId = $('#seckill-voucher-id')?.value;
    if (!voucherId) { toast('请先选择景区门票', 'error'); return; }
    if (!confirm('确认立即抢票？此操作不可撤销。')) return;
    const btn = $('#seckill-btn');
    if (btn) { btn.disabled=true; btn.innerHTML='<div class="spinner"></div> 抢票中...'; }
    $('#seckill-result').innerHTML = '';
    try {
      const r = await api('/api/seckill/execute', {
        method: 'POST',
        body: JSON.stringify({ voucherId: Number(voucherId), userId: Number(state.user.userId) })
      });
      console.log('[Seckill] result:', r);
      const d = parseData(r) || {};
      const orderId = d.orderId || d.order_id || '处理中';
      toast('🎉 抢票成功！', 'success');
      $('#seckill-result').innerHTML = `
        <div style="text-align:center;padding:20px;background:rgba(45,212,191,.06);border:1px solid rgba(45,212,191,.2);border-radius:var(--radius-md);margin-top:12px">
          <div style="font-size:2rem;margin-bottom:8px">🎊</div>
          <div style="font-size:1rem;font-weight:700;color:var(--teal)">恭喜！抢票成功</div>
          <div style="font-size:.76rem;color:var(--text-muted);margin-top:6px">订单号: ${orderId}</div>
          <button class="btn btn-ghost btn-sm" style="margin-top:12px" onclick="showPage('orders')">查看我的订单</button>
        </div>`;
    } catch(e) {
      toast('抢票失败: ' + e.message, 'error');
      $('#seckill-result').innerHTML = `<div style="color:var(--rose);font-size:.82rem;padding:8px 0">抢票失败：${e.message}</div>`;
    } finally {
      if (btn) { btn.disabled=false; btn.textContent='🎫 立即抢票'; }
    }
  });
}

async function initStockFromInput() {
  const voucherId = $('#init-voucher-id')?.value;
  const stock     = $('#init-stock-num')?.value;
  if (!voucherId || !stock) { toast('请填写优惠券ID和库存数量', 'error'); return; }
  try {
    await api('/api/seckill/init-stock', {
      method: 'POST',
      body: JSON.stringify({ voucherId: Number(voucherId), stock: Number(stock) })
    });
    toast(`库存初始化成功：ID=${voucherId}, 库存=${stock}张`, 'success');
  } catch(e) { toast('初始化失败: ' + e.message, 'error'); }
}
