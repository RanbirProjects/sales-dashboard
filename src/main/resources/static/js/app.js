(() => {
  const $ = (s) => document.querySelector(s);
  const money = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
  const money2 = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n);
  const int = (n) => new Intl.NumberFormat('en-IN').format(n);

  const PALETTE = ['#00f0ff', '#0072ff', '#7c3aed', '#22c55e', '#f59e0b', '#ff5470', '#38bdf8', '#a3e635'];
  Chart.defaults.color = '#9fb7d9';
  Chart.defaults.font.family = "'Segoe UI', system-ui, sans-serif";
  Chart.defaults.borderColor = 'rgba(255,255,255,0.06)';

  const charts = {};
  let sales = [];
  let editingId = null;

  /* ---------- helpers ---------- */
  async function api(path, opts = {}) {
    const res = await fetch('/api' + path, { headers: { 'Content-Type': 'application/json' }, ...opts });
    if (!res.ok) {
      let msg = 'Request failed (' + res.status + ')';
      try { const j = await res.json(); msg = j.message || msg; } catch (e) { /* ignore */ }
      throw new Error(msg);
    }
    return res.status === 204 ? null : res.json();
  }

  function query() {
    const p = new URLSearchParams();
    [['region', '#f-region'], ['category', '#f-category'], ['from', '#f-from'], ['to', '#f-to']].forEach(([k, sel]) => {
      const v = $(sel).value;
      if (v) p.set(k, v);
    });
    const s = p.toString();
    return s ? '?' + s : '';
  }

  function toast(msg, isErr = false) {
    const t = $('#toast');
    t.textContent = msg;
    t.className = 'toast show' + (isErr ? ' err' : '');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => (t.className = 'toast'), 2600);
  }

  function countUp(el, target, format) {
    const from = Number(el.dataset.v || 0);
    el.dataset.v = target;
    const start = performance.now();
    const dur = 1100;
    const step = (now) => {
      const p = Math.min((now - start) / dur, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      el.textContent = format(from + (target - from) * eased);
      if (p < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  function monthLabel(ym) {
    const [y, m] = ym.split('-');
    return new Date(+y, +m - 1, 1).toLocaleString('en-IN', { month: 'short', year: '2-digit' });
  }

  /* ---------- charts ---------- */
  function upsert(id, config) {
    if (charts[id]) {
      charts[id].data = config.data;
      charts[id].update();
    } else {
      charts[id] = new Chart($('#' + id), config);
    }
  }

  const tooltipMoney = { callbacks: { label: (c) => ' ' + money(c.parsed.y ?? c.parsed.x ?? c.parsed) } };

  function renderCharts(d) {
    const ctx = $('#c-monthly').getContext('2d');
    const grad = ctx.createLinearGradient(0, 0, 0, 300);
    grad.addColorStop(0, 'rgba(0,240,255,0.45)');
    grad.addColorStop(1, 'rgba(0,114,255,0)');

    upsert('c-monthly', {
      type: 'line',
      data: {
        labels: d.monthly.map((m) => monthLabel(m.label)),
        datasets: [{ data: d.monthly.map((m) => m.value), borderColor: '#00f0ff', backgroundColor: grad, fill: true, tension: 0.4, pointRadius: 4, pointBackgroundColor: '#00f0ff', borderWidth: 3 }],
      },
      options: {
        maintainAspectRatio: false,
        animation: { duration: 1400, easing: 'easeOutQuart' },
        plugins: { legend: { display: false }, tooltip: tooltipMoney },
        scales: { y: { ticks: { callback: (v) => '₹' + (v / 100000).toFixed(1) + 'L' } } },
      },
    });

    upsert('c-category', {
      type: 'doughnut',
      data: { labels: d.byCategory.map((x) => x.label), datasets: [{ data: d.byCategory.map((x) => x.value), backgroundColor: PALETTE, borderWidth: 0, hoverOffset: 12 }] },
      options: {
        maintainAspectRatio: false, cutout: '62%',
        animation: { animateRotate: true, animateScale: true, duration: 1400 },
        plugins: { legend: { position: 'bottom' }, tooltip: { callbacks: { label: (c) => ' ' + c.label + ': ' + money(c.parsed) } } },
      },
    });

    const bar = (id, data, horizontal) =>
      upsert(id, {
        type: 'bar',
        data: { labels: data.map((x) => x.label), datasets: [{ data: data.map((x) => x.value), backgroundColor: PALETTE, borderRadius: 8, borderSkipped: false }] },
        options: {
          indexAxis: horizontal ? 'y' : 'x', maintainAspectRatio: false,
          animation: { duration: 1200, easing: 'easeOutQuart' },
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => ' ' + money(horizontal ? c.parsed.x : c.parsed.y) } } },
          scales: { [horizontal ? 'x' : 'y']: { ticks: { callback: (v) => '₹' + (v / 100000).toFixed(0) + 'L' } } },
        },
      });

    bar('c-region', d.byRegion, false);
    bar('c-products', d.topProducts, true);
    bar('c-people', d.topSalespeople, true);
  }

  /* ---------- table ---------- */
  function renderTable() {
    const term = $('#search').value.trim().toLowerCase();
    const list = sales.filter((s) => !term || [s.product, s.salesperson, s.region, s.category].some((v) => v.toLowerCase().includes(term)));
    const shown = list.slice(0, 100);
    $('#rowCount').textContent = '(showing ' + shown.length + ' of ' + list.length + ')';
    $('#rows').innerHTML = shown.map((s, i) => `
      <tr data-id="${s.id}" style="--r:${i}">
        <td>${s.orderDate}</td>
        <td><strong>${esc(s.product)}</strong></td>
        <td><span class="tag">${esc(s.category)}</span></td>
        <td>${esc(s.region)}</td>
        <td>${esc(s.salesperson)}</td>
        <td class="r">${s.quantity}</td>
        <td class="r">${money2(s.unitPrice)}</td>
        <td class="r"><strong>${money2(s.total)}</strong></td>
        <td class="r"><button class="icon-btn" data-act="edit" title="Edit">✏️</button><button class="icon-btn" data-act="del" title="Delete">🗑️</button></td>
      </tr>`).join('');
  }

  function esc(v) {
    return String(v).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  /* ---------- data loading ---------- */
  async function refresh() {
    const q = query();
    const [dash, list] = await Promise.all([api('/dashboard' + q), api('/sales' + q)]);
    countUp($('#k-revenue'), Number(dash.totalRevenue), money);
    countUp($('#k-orders'), dash.totalOrders, int);
    countUp($('#k-units'), dash.totalUnits, int);
    countUp($('#k-avg'), Number(dash.avgOrderValue), money);
    renderCharts(dash);
    sales = list;
    renderTable();
    $('#exportBtn').href = '/api/sales/export' + q;
  }

  async function loadFilters() {
    const f = await api('/filters');
    const fill = (sel, items, keepFirst) => {
      const el = $(sel);
      el.innerHTML = (keepFirst ? el.firstElementChild.outerHTML : '') + items.map((v) => `<option>${esc(v)}</option>`).join('');
    };
    fill('#f-region', f.regions, true);
    fill('#f-category', f.categories, true);
    const dl = (id, items) => ($(id).innerHTML = items.map((v) => `<option value="${esc(v)}">`).join(''));
    dl('#dl-products', f.products); dl('#dl-categories', f.categories); dl('#dl-regions', f.regions); dl('#dl-people', f.salespeople);
  }

  /* ---------- modal ---------- */
  function openModal(sale) {
    editingId = sale ? sale.id : null;
    $('#modalTitle').textContent = sale ? 'Edit sale' : 'Add sale';
    $('#m-date').value = sale ? sale.orderDate : new Date().toISOString().slice(0, 10);
    $('#m-product').value = sale ? sale.product : '';
    $('#m-category').value = sale ? sale.category : '';
    $('#m-region').value = sale ? sale.region : '';
    $('#m-person').value = sale ? sale.salesperson : '';
    $('#m-qty').value = sale ? sale.quantity : 1;
    $('#m-price').value = sale ? sale.unitPrice : '';
    $('#formError').textContent = '';
    updateTotal();
    $('#modal').classList.add('open');
  }
  const closeModal = () => $('#modal').classList.remove('open');
  const updateTotal = () => ($('#m-total').textContent = money2((+$('#m-qty').value || 0) * (+$('#m-price').value || 0)));

  async function submitForm(e) {
    e.preventDefault();
    const body = {
      orderDate: $('#m-date').value,
      product: $('#m-product').value.trim(),
      category: $('#m-category').value.trim(),
      region: $('#m-region').value.trim(),
      salesperson: $('#m-person').value.trim(),
      quantity: parseInt($('#m-qty').value, 10),
      unitPrice: parseFloat($('#m-price').value),
    };
    try {
      if (editingId) await api('/sales/' + editingId, { method: 'PUT', body: JSON.stringify(body) });
      else await api('/sales', { method: 'POST', body: JSON.stringify(body) });
      closeModal();
      toast(editingId ? 'Sale updated ✓' : 'Sale added ✓');
      await loadFilters();
      await refresh();
    } catch (err) {
      $('#formError').textContent = err.message;
      const card = $('.modal-card');
      card.classList.remove('shake'); void card.offsetWidth; card.classList.add('shake');
    }
  }

  /* ---------- events ---------- */
  $('#addBtn').addEventListener('click', () => openModal(null));
  $('#cancelBtn').addEventListener('click', closeModal);
  $('#modal').addEventListener('click', (e) => { if (e.target.id === 'modal') closeModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });
  $('#saleForm').addEventListener('submit', submitForm);
  ['#m-qty', '#m-price'].forEach((s) => $(s).addEventListener('input', updateTotal));
  ['#f-region', '#f-category', '#f-from', '#f-to'].forEach((s) => $(s).addEventListener('change', () => refresh().catch((e) => toast(e.message, true))));
  $('#resetBtn').addEventListener('click', () => {
    ['#f-region', '#f-category', '#f-from', '#f-to'].forEach((s) => ($(s).value = ''));
    refresh().catch((e) => toast(e.message, true));
  });
  $('#search').addEventListener('input', renderTable);

  $('#rows').addEventListener('click', async (e) => {
    const btn = e.target.closest('button[data-act]');
    if (!btn) return;
    const tr = btn.closest('tr');
    const sale = sales.find((s) => String(s.id) === tr.dataset.id);
    if (btn.dataset.act === 'edit') return openModal(sale);
    if (!confirm('Delete this sale record?')) return;
    try {
      tr.classList.add('removing');
      await api('/sales/' + sale.id, { method: 'DELETE' });
      setTimeout(refresh, 320);
      toast('Sale deleted');
    } catch (err) {
      tr.classList.remove('removing');
      toast(err.message, true);
    }
  });

  /* ---------- start ---------- */
  loadFilters().then(refresh).catch((e) => toast('Could not load data: ' + e.message, true));
})();
