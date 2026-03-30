// ===== 订单页 =====
async function initOrders() {
  const box = $('#orders-list');
  if (!box) return;
  if (!state.user || !state.user.userId) {
    box.innerHTML = `
      <div class="empty-state">
        <span class="icon">🔐</span>
        <p>请先登录查看订单</p>
        <button class="btn btn-primary btn-sm" style="margin-top:12px" onclick="openLoginModal()">登录</button>
      </div>`;
    return;
  }
  console.log('[Orders] userId:', state.user.userId);
  await loadOrders();
}

async function loadOrders(page = 1) {
  const box = $('#orders-list');
  if (!box) return;
  box.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';
  try {
    const userId = state.user.userId;
    const url = `/api/orders/user/${userId}?page=${page}&size=10`;
    console.log('[Orders] request url:', url);
    const r = await api(url);
    console.log('[Orders] raw response:', JSON.stringify(r));

    // 后端返回 data 是字符串（JSON），需二次解析
    let d = r.data;
    if (typeof d === 'string') {
      try { d = JSON.parse(d); } catch(e) { console.warn('[Orders] parse failed:', e); }
    }
    console.log('[Orders] parsed data:', d);

    const records = d?.records || d?.list || (Array.isArray(d) ? d : []);
    console.log('[Orders] records count:', records.length);

    if (!records.length) {
      box.innerHTML = `
        <div class="empty-state">
          <span class="icon">📦</span>
          <p>暂无订单记录</p>
          <p style="font-size:.75rem;color:var(--text-muted)">(userId: ${userId})</p>
          <button class="btn btn-primary btn-sm" style="margin-top:12px" onclick="showPage('seckill')">去抢票</button>
        </div>`;
      return;
    }

    box.innerHTML = records.map(o => `
      <div class="card order-card">
        <div class="order-header">
          <span class="order-id"># ${o.orderId || o.id}</span>
          ${statusBadge(o.status)}
        </div>
        <div class="order-body">
          <div class="order-icon" style="background:${randBg(o.voucherId||0)}">🎫</div>
          <div class="order-detail">
            <div class="order-name">${o.attractionName || '景区门票 #' + (o.voucherId||o.attractionId||'—')}</div>
            <div class="order-time">${o.createTime||''}</div>
          </div>
        </div>
        <div class="order-footer">
          <span style="font-size:.73rem;color:var(--text-muted)">${o.statusDesc||''}</span>
          ${(o.status===1||o.status===2)
            ? `<button class="btn btn-ghost btn-sm" style="color:var(--rose)" onclick="cancelOrder(${o.orderId||o.id})">取消订单</button>`
            : ''}
        </div>
      </div>`).join('');

    // 分页
    const totalPages = d?.pages || d?.totalPages || 1;
    if (totalPages > 1) {
      box.insertAdjacentHTML('beforeend', `
        <div style="display:flex;justify-content:center;align-items:center;gap:10px;margin-top:20px">
          ${page>1 ? `<button class="btn btn-ghost btn-sm" onclick="loadOrders(${page-1})">◀ 上一页</button>` : ''}
          <span style="font-size:.8rem;color:var(--text-muted)">${page} / ${totalPages}</span>
          ${page<totalPages ? `<button class="btn btn-ghost btn-sm" onclick="loadOrders(${page+1})">下一页 ▶</button>` : ''}
        </div>`);
    }
  } catch(e) {
    console.error('[Orders] error:', e);
    box.innerHTML = `<div class="empty-state"><span class="icon">⚠️</span><p>加载订单失败: ${e.message}</p></div>`;
  }
}

async function cancelOrder(orderId) {
  if (!confirm('确认取消此订单？取消后库存将自动回补。')) return;
  try {
    await api(`/api/orders/${orderId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ userId: Number(state.user.userId) })
    });
    toast('订单已取消，库存已回补 ✓', 'success');
    loadOrders();
  } catch(e) { toast('取消失败: ' + e.message, 'error'); }
}
