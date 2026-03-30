// ===== 探索页 =====
async function initExplore() {
  await searchAttractions(
    $('#explore-city')?.value || '',
    $('#explore-keyword')?.value || ''
  );
}

async function searchAttractions(city, keyword) {
  const box = $('#explore-list');
  if (!box) return;
  box.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';
  try {
    const p = new URLSearchParams();
    if (city)    p.set('city', city);
    if (keyword) p.set('keyword', keyword);
    const r = await api('/api/attractions/search?' + p);
    const list = parseData(r) || [];
    if (!list.length) {
      box.innerHTML = '<div class="empty-state"><span class="icon">🔍</span><p>未找到相关景区，换个关键词试试</p></div>';
      return;
    }
    box.innerHTML = '<div class="grid-3">' + list.map(a => attractionCard(a)).join('') + '</div>';
  } catch(e) {
    box.innerHTML = `<div class="empty-state"><span class="icon">⚠️</span><p>搜索失败: ${e.message}</p></div>`;
  }
}
