// ===== 首页 =====
async function initHome() {
  loadHotRank();
  loadFeaturedAttractions();
  seedHotRankIfEmpty();
}

// 若热榜为空则预填充访问热度
async function seedHotRankIfEmpty() {
  try {
    const r = await api('/api/rank/hot?topN=3');
    const list = parseData(r);
    if (!list || list.length === 0) {
      // 对前5个景区记录访问，让热榜有数据
      const ar = await api('/api/attractions/search?city=&keyword=');
      const attractions = parseData(ar) || [];
      for (const a of attractions.slice(0, 8)) {
        for (let i = 0; i < 3; i++) {
          api(`/api/rank/visit?attractionId=${a.id}&userId=seed${i}`, {method:'POST'}).catch(()=>{});
        }
      }
      setTimeout(loadHotRank, 1500);
    }
  } catch {}
}

async function loadHotRank() {
  const box = $('#rank-list');
  if (!box) return;
  box.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';
  try {
    const r = await api('/api/rank/hot?topN=8');
    const list = parseData(r);
    if (!list || !list.length) {
      box.innerHTML = '<div class="empty-state"><span class="icon">📊</span><p>热榜数据生成中，请稍后刷新</p></div>';
      return;
    }
    box.innerHTML = list.map((a, i) => `
      <div class="rank-item" style="cursor:pointer" onclick="openAttractionDetail(${a.attractionId||a.id})">
        <span class="rank-num ${i<3?'top':''}">${i+1}</span>
        <div class="rank-thumb" style="background:${randBg(a.attractionId||a.id)}">${randThumb(a.attractionId||a.id)}</div>
        <div class="rank-info">
          <div class="rank-name">${a.name||'未知景区'}</div>
          <div class="rank-city">📍 ${a.city||''}</div>
        </div>
        <span class="rank-hot">🔥 ${Math.round(a.hotScore||0)}</span>
      </div>`).join('');
  } catch(e) {
    box.innerHTML = `<div class="empty-state"><span class="icon">⚠️</span><p>热榜加载失败: ${e.message}</p></div>`;
  }
}

async function loadFeaturedAttractions() {
  const box = $('#featured-list');
  if (!box) return;
  box.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';
  try {
    const r = await api('/api/attractions/search?city=&keyword=');
    const list = parseData(r) || [];
    if (!list.length) {
      box.innerHTML = '<div class="empty-state"><span class="icon">🗺</span><p>暂无景区数据</p></div>';
      return;
    }
    box.innerHTML = list.slice(0,6).map(a => attractionCard(a)).join('');
  } catch(e) {
    box.innerHTML = `<div class="empty-state"><span class="icon">⚠️</span><p>景区加载失败: ${e.message}</p></div>`;
  }
}

async function openAttractionDetail(id) {
  if (!id) return;
  const modal = document.createElement('div');
  modal.className = 'modal-overlay';
  modal.innerHTML = '<div class="modal"><div class="loading-overlay"><div class="spinner"></div><p>加载中...</p></div></div>';
  modal.addEventListener('click', e => { if(e.target===modal) modal.remove(); });
  document.body.appendChild(modal);
  try {
    const [dr, sr] = await Promise.all([
      api(`/api/attractions/${id}`),
      api(`/api/attractions/${id}/stock`),
    ]);
    const a     = parseData(dr) || {};
    const sdata = parseData(sr) || {};
    const stock = sdata.stock ?? 0;
    // 记录访问热度
    api(`/api/rank/visit?attractionId=${id}&userId=${state.user?.userId||'anon'}`,{method:'POST'}).catch(()=>{});
    modal.querySelector('.modal').innerHTML = `
      <div class="modal-header">
        <h2 class="modal-title">${randThumb(id)} ${a.name||'景区详情'}</h2>
        <button class="modal-close" onclick="this.closest('.modal-overlay').remove()">✕</button>
      </div>
      <div style="display:flex;flex-direction:column;gap:16px">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <span class="tag">📍 ${a.city||'—'}</span>
          <span class="tag">⏰ ${a.openHours||'全天'}</span>
          <span class="tag">⭐ ${a.score||'—'}</span>
          <span class="badge ${stock>0?'badge-teal':'badge-rose'}">${stock>0?'🎫 余票 '+stock+'张':'售罄'}</span>
        </div>
        <p style="font-size:.87rem;color:var(--text-secondary);line-height:1.75">${a.description||'暂无简介'}</p>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-size:1.5rem;font-weight:800;color:var(--accent)">¥${a.price||0} <span style="font-size:.78rem;font-weight:400;color:var(--text-muted)">/人</span></span>
          <div style="display:flex;gap:10px">
            ${stock>0
              ? `<button class="btn btn-primary btn-sm" onclick="goSeckill(${id})">🎫 立即抢票</button>`
              : `<button class="btn btn-ghost btn-sm" onclick="doSubscribeFromDetail(${id})">🔔 订阅提醒</button>`}
          </div>
        </div>
        <div style="font-size:.75rem;color:var(--text-muted)">地址：${a.address||'—'}</div>
      </div>`;
  } catch(e) {
    modal.querySelector('.modal').innerHTML = `<p style="color:var(--rose);padding:20px">加载失败: ${e.message}</p><button class="btn btn-ghost btn-sm" onclick="this.closest('.modal-overlay').remove()">关闭</button>`;
  }
}

function goSeckill(attractionId) {
  document.querySelector('.modal-overlay')?.remove();
  showPage('seckill');
  // 等待秒杀页初始化完成后，高亮选中对应卡片
  setTimeout(() => {
    const card = document.getElementById(`voucher-card-${attractionId}`);
    if (card) {
      card.scrollIntoView({ behavior:'smooth', block:'center' });
      card.click();
    } else {
      // 如果列表中没有该卡片，直接填入ID
      const inp = $('#seckill-voucher-id');
      if (inp) inp.value = attractionId;
      const nm = $('#selected-voucher-name');
      if (nm) nm.innerHTML = `<span style="color:var(--text-muted)">ID: ${attractionId}</span>`;
    }
  }, 400);
}

async function doSubscribeFromDetail(attractionId) {
  requireLogin(async () => {
    try {
      const r = await api(`/api/notify/subscribe?voucherId=${attractionId}&userId=${state.user.userId}`,{method:'POST'});
      const d = parseData(r);
      toast(d?.message || '订阅成功 🔔', 'success');
    } catch { toast('订阅失败', 'error'); }
  });
}
