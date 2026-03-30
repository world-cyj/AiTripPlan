// ===== AI 规划页 =====
let sseController = null;

function initPlan() {
  if (!state.currentConvId) {
    state.currentConvId = 'conv-' + Date.now();
  }
  // 检测 Agent 服务状态
  checkAgentStatus();
  renderChatHistory();
}

async function checkAgentStatus() {
  const statusEl = $('#agent-status');
  if (!statusEl) return;
  try {
    const r = await fetch(`${API.agent}/actuator/health`, {
      signal: AbortSignal.timeout(3000)
    });
    const d = await r.json();
    if (d.status === 'UP') {
      statusEl.innerHTML = '<span style="color:var(--teal)">✓ Agent 服务已连接</span>';
      statusEl.style.background = 'rgba(45,212,191,.06)';
      statusEl.style.borderColor = 'rgba(45,212,191,.2)';
    } else {
      statusEl.innerHTML = `<span style="color:var(--rose)">✗ Agent 状态异常: ${d.status}</span>`;
    }
  } catch {
    statusEl.innerHTML = `<span style="color:var(--rose)">✗ Agent 未连接（端口 ${CONFIG.agentPort}）</span> <button class="tag" style="cursor:pointer;font-size:.7rem" onclick="openPortSettings()">⚙️ 修改端口</button>`;
    statusEl.style.background = 'rgba(251,113,133,.06)';
    statusEl.style.borderColor = 'rgba(251,113,133,.2)';
  }
}

const chatHistory = [];

function renderChatHistory() {
  const box = $('#chat-messages');
  if (!box) return;
  if (!chatHistory.length) {
    box.innerHTML = `
      <div style="text-align:center;padding:40px 20px;color:var(--text-muted)">
        <div style="font-size:2.5rem;margin-bottom:12px">✈️</div>
        <p style="font-size:.9rem;color:var(--text-primary)">你好！我是 AiTrip 旅行规划师</p>
        <p style="font-size:.8rem;margin-top:6px">告诉我你想去哪里，我来帮你规划行程 🗺</p>
        <div style="margin-top:16px;padding:12px;background:rgba(251,191,36,.06);border:1px solid rgba(251,191,36,.15);border-radius:8px;font-size:.76rem;line-height:1.8;text-align:left">
          <strong style="color:var(--accent)">💡 使用说明：</strong><br>
          • 需要先在 IDEA 启动 <code>AiTripAgentApplication</code>（端口 ${CONFIG.agentPort}）<br>
          • 需要配置阿里云 DashScope API Key<br>
          • 点右上角 ⚙️ 可修改 Agent 端口
        </div>
      </div>`;
    return;
  }
  box.innerHTML = chatHistory.map(m => `
    <div style="display:flex;flex-direction:column;align-items:${m.role==='user'?'flex-end':'flex-start'};margin-bottom:16px">
      <div style="max-width:82%;padding:12px 16px;
        border-radius:${m.role==='user'?'16px 16px 4px 16px':'16px 16px 16px 4px'};
        background:${m.role==='user'?'linear-gradient(135deg,var(--accent),var(--accent-dim))':'var(--bg-card)'};
        color:${m.role==='user'?'#0a0e1a':'var(--text-primary)'};
        font-size:.88rem;line-height:1.75;
        border:${m.role==='user'?'none':'1px solid var(--border)'};
        white-space:pre-wrap;word-break:break-word">${escapeHtml(m.content)}${m.streaming?'<span class="cursor"></span>':''}</div>
    </div>`).join('');
  box.scrollTop = box.scrollHeight;
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function sendPlanMessage() {
  const inp = $('#plan-input');
  const msg = inp?.value.trim();
  if (!msg) return;

  // 取消上一次请求
  if (sseController) { sseController.abort(); sseController = null; }

  chatHistory.push({ role: 'user', content: msg });
  if (inp) inp.value = '';
  chatHistory.push({ role: 'assistant', content: '', streaming: true });
  const aiIdx = chatHistory.length - 1;
  renderChatHistory();

  const userId = state.user?.userId || 'guest';
  const params = new URLSearchParams({
    message: msg,
    userId: String(userId),
    conversationId: state.currentConvId
  });
  const url = `${API.agent}/api/agent/plan?${params}`;
  console.log('[Plan] connecting:', url);

  sseController = new AbortController();

  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: { 'Accept': 'text/event-stream', 'Cache-Control': 'no-cache' },
      signal: sseController.signal
    });

    if (!resp.ok) {
      const errText = await resp.text().catch(() => '');
      throw new Error(`HTTP ${resp.status} - ${errText.slice(0, 300)}`);
    }

    const reader = resp.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      // 按 SSE 规范处理：\n\n 分隔事件
      const events = buffer.split('\n\n');
      buffer = events.pop() || ''; // 保留不完整的部分

      for (const event of events) {
        const lines = event.split('\n');
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const text = line.slice(5); // 保留原始空格（不trim，避免丢失空格）
            if (text !== '[DONE]' && text !== '') {
              chatHistory[aiIdx].content += text;
              renderChatHistory();
            }
          }
        }
      }
    }

    // 处理剩余 buffer
    if (buffer) {
      const lines = buffer.split('\n');
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const text = line.slice(5);
          if (text !== '[DONE]' && text !== '') {
            chatHistory[aiIdx].content += text;
          }
        }
      }
    }

  } catch(e) {
    if (e.name === 'AbortError') {
      console.log('[Plan] SSE aborted by user');
      return;
    }
    console.error('[Plan] SSE error:', e);
    let errMsg = '';
    if (e.message.includes('Failed to fetch') || e.message.includes('NetworkError') || e.message.includes('fetch')) {
      errMsg = `\n\n⚠️ 无法连接到 Agent 服务\n\n请确认：\n1. 已在 IDEA 启动 AiTripAgentApplication\n2. Agent 端口为 ${CONFIG.agentPort}（点 ⚙️ 修改）\n3. 已配置 DashScope API Key`;
    } else if (e.name === 'TimeoutError') {
      errMsg = '\n\n⚠️ 请求超时，Agent 可能正在处理中，请稍后重试';
    } else {
      errMsg = `\n\n⚠️ 错误: ${e.message}`;
    }
    chatHistory[aiIdx].content += errMsg;
  } finally {
    chatHistory[aiIdx].streaming = false;
    sseController = null;
    renderChatHistory();
  }
}

function clearPlan() {
  if (sseController) { sseController.abort(); sseController = null; }
  chatHistory.length = 0;
  state.currentConvId = 'conv-' + Date.now();
  renderChatHistory();
  toast('已开启新对话', 'info');
}

function sendQuickPrompt(text) {
  const inp = $('#plan-input');
  if (inp) { inp.value = text; inp.focus(); }
  sendPlanMessage();
}
