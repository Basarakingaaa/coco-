const api = {
  get: (url) => $.getJSON(`api${url}`).then(r => r.data),
  post: (url, data) => $.ajax({ url: `api${url}`, method: 'POST', contentType: 'application/json', data: JSON.stringify(data || {}) }).then(r => r.data),
  put: (url, data) => $.ajax({ url: `api${url}`, method: 'PUT', contentType: 'application/json', data: JSON.stringify(data || {}) }).then(r => r.data),
  del: (url, data) => $.ajax({ url: `api${url}`, method: 'DELETE', contentType: 'application/json', data: JSON.stringify(data || {}) }).then(r => r.data)
};

let currentUser = null;
let categories = [];

$(async function () {
  bindShell();
  await loadSession();
  window.addEventListener('hashchange', route);
  route();
});

function bindShell() {
  $('#loginLink').on('click', loginModal);
  $('#logoutLink').on('click', async () => {
    await api.post('/users/logout');
    currentUser = null;
    refreshSessionUi();
    location.hash = '#/';
  });
  $('#closeModal').on('click', closeModal);
  $('#modal').on('click', e => { if (e.target.id === 'modal') closeModal(); });
}

async function loadSession() {
  currentUser = await api.get('/session');
  refreshSessionUi();
  if (currentUser) await refreshCartBadge();
}

function refreshSessionUi() {
  $('#loginLink').toggleClass('hidden', !!currentUser).text(currentUser ? currentUser.username : '登录');
  $('#logoutLink').toggleClass('hidden', !currentUser);
}

async function route() {
  const hash = location.hash || '#/';
  try {
    if (hash === '#/') await renderHome();
    else if (hash.startsWith('#/products/')) await renderProductDetail(hash.split('/')[2]);
    else if (hash.startsWith('#/products')) await renderProducts();
    else if (hash === '#/cart') await requireLogin(renderCart);
    else if (hash === '#/checkout') await requireLogin(renderCheckout);
    else if (hash.startsWith('#/orders/')) await requireLogin(() => renderOrderDetail(hash.split('/')[2]));
    else if (hash === '#/orders') await requireLogin(renderOrders);
    else if (hash.startsWith('#/admin')) await requireAdmin(renderAdmin);
    else await renderHome();
  } catch (error) {
    showError(error);
  }
}

async function renderHome() {
  const ads = await api.get('/ads');
  const products = await api.get('/products');
  const cats = await api.get('/categories');
  $('#app').html(`
    <section class="hero">
      <div class="hero-eyebrow">精选电商系统</div>
      <h1 class="hero-title">好物、分类、订单一站式管理。</h1>
      <p class="hero-subtitle">Maven WebApp + jQuery + 纯 HTML 重写版，视觉风格保持原项目。</p>
      <a class="btn btn-primary" href="#/products">浏览商品</a>
    </section>
    <section class="section container">
      <h2 class="section-title">首页广告</h2>
      <div class="grid grid-3">${ads.slice(0, 3).map(adCard).join('') || empty('暂无广告')}</div>
    </section>
    <section class="section container">
      <h2 class="section-title">商品分类</h2>
      <div class="grid grid-4">${cats.slice(0, 8).map(c => `<a class="card card-body" href="#/products?categoryId=${c.id}"><strong>${esc(c.name)}</strong><p class="muted">查看分类商品</p></a>`).join('')}</div>
    </section>
    <section class="section container">
      <h2 class="section-title">推荐商品</h2>
      <div class="grid grid-3">${products.slice(0, 6).map(productCard).join('')}</div>
    </section>
  `);
}

async function renderProducts() {
  categories = await api.get('/categories');
  const params = new URLSearchParams((location.hash.split('?')[1] || ''));
  const query = [];
  if (params.get('categoryId')) query.push(`categoryId=${params.get('categoryId')}`);
  if (params.get('keyword')) query.push(`keyword=${encodeURIComponent(params.get('keyword'))}`);
  const products = await api.get(`/products${query.length ? '?' + query.join('&') : ''}`);
  $('#app').html(`
    <section class="section container">
      <h1 class="section-title">商品列表</h1>
      <div class="toolbar">
        <select id="categoryFilter"><option value="">全部分类</option>${categories.map(c => `<option value="${c.id}" ${String(c.id) === params.get('categoryId') ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}</select>
        <input id="keywordFilter" placeholder="搜索商品" value="${esc(params.get('keyword') || '')}">
        <button class="btn btn-primary" id="searchBtn">搜索</button>
      </div>
      <div class="grid grid-3">${products.map(productCard).join('') || empty('暂无商品')}</div>
    </section>
  `);
  $('#searchBtn').on('click', () => {
    const q = new URLSearchParams();
    if ($('#categoryFilter').val()) q.set('categoryId', $('#categoryFilter').val());
    if ($('#keywordFilter').val()) q.set('keyword', $('#keywordFilter').val());
    location.hash = `#/products${q.toString() ? '?' + q.toString() : ''}`;
  });
}

async function renderProductDetail(id) {
  const p = await api.get(`/products/${id}`);
  const cover = firstMedia(p) || 'https://dummyimage.com/900x520/f5f5f7/86868b&text=Product';
  $('#app').html(`
    <section class="section container grid grid-2">
      <div class="card"><img class="product-img" style="height:520px" src="${esc(cover)}" alt=""></div>
      <div>
        <p class="muted">${esc(p.category_name || '')}</p>
        <h1>${esc(p.name)}</h1>
        <p class="muted">${esc(p.subtitle || '')}</p>
        <p class="price">￥${p.price}</p>
        <p>库存：${p.stock}　销量：${p.sales}</p>
        <button class="btn btn-primary" id="addCart">加入购物车</button>
      </div>
    </section>
    <section class="section container">
      <h2 class="section-title">商品资料</h2>
      <div class="grid grid-3">${(p.media || []).map(m => `<div class="card"><img class="product-img" src="${esc(m.url)}"><div class="card-body">${esc(m.media_type)}</div></div>`).join('') || empty('暂无资料')}</div>
    </section>
    <section class="section container">
      <h2 class="section-title">规格参数</h2>
      <table class="table">${(p.infos2 || []).map(i => `<tr><th>${esc(i.spec_key)}</th><td>${esc(i.spec_value)}</td></tr>`).join('') || '<tr><td>暂无参数</td></tr>'}</table>
    </section>
  `);
  $('#addCart').on('click', async () => {
    await requireLogin(async () => {
      await api.post('/cart', { productId: p.id, quantity: 1 });
      await refreshCartBadge();
      alert('已加入购物车');
    });
  });
}

async function renderCart() {
  const rows = await api.get('/cart');
  $('#app').html(`
    <section class="section container">
      <h1 class="section-title">购物车</h1>
      <table class="table">
        <thead><tr><th>商品</th><th>价格</th><th>数量</th><th>操作</th></tr></thead>
        <tbody>${rows.map(r => `<tr><td>${esc(r.product_name)}</td><td>￥${r.price}</td><td><input class="qty" data-id="${r.id}" value="${r.quantity}" style="width:80px"></td><td><button class="btn btn-danger remove" data-id="${r.id}">删除</button></td></tr>`).join('') || '<tr><td colspan="4">购物车为空</td></tr>'}</tbody>
      </table>
      <p><a class="btn btn-primary" href="#/checkout">去结算</a></p>
    </section>
  `);
  $('.qty').on('change', async function () { await api.put(`/cart/${$(this).data('id')}`, { quantity: Number($(this).val()) }); await refreshCartBadge(); });
  $('.remove').on('click', async function () { await api.del('/cart', { ids: [$(this).data('id')] }); await renderCart(); await refreshCartBadge(); });
}

async function renderCheckout() {
  $('#app').html(`
    <section class="section container">
      <h1 class="section-title">结算下单</h1>
      <form class="form card card-body" id="checkoutForm">
        <label>收货人<input name="receiverName" required value="张三"></label>
        <label>手机号<input name="receiverPhone" required value="13800000000"></label>
        <label>收货地址<input name="receiverAddress" required value="北京市朝阳区"></label>
        <button class="btn btn-primary">提交订单</button>
      </form>
    </section>
  `);
  $('#checkoutForm').on('submit', async e => {
    e.preventDefault();
    const order = await api.post('/orders/checkout', formData(e.target));
    await refreshCartBadge();
    location.hash = `#/orders/${order.id}`;
  });
}

async function renderOrders() {
  const orders = await api.get('/orders');
  $('#app').html(`<section class="section container"><h1 class="section-title">我的订单</h1>${orderTable(orders, false)}</section>`);
}

async function renderOrderDetail(id) {
  const o = await api.get(`/orders/${id}`);
  $('#app').html(`
    <section class="section container">
      <h1 class="section-title">订单 ${esc(o.order_no)}</h1>
      <div class="notice">状态：${statusText(o.status)}　金额：￥${o.total_amount}</div>
      <table class="table"><thead><tr><th>商品</th><th>价格</th><th>数量</th></tr></thead><tbody>${(o.items || []).map(i => `<tr><td>${esc(i.product_name)}</td><td>￥${i.price}</td><td>${i.quantity}</td></tr>`).join('')}</tbody></table>
      <p><button class="btn btn-primary" id="payBtn">模拟支付</button> <button class="btn btn-light" id="cancelBtn">取消订单</button></p>
    </section>
  `);
  $('#payBtn').on('click', async () => { await api.post(`/orders/${id}/pay`); await renderOrderDetail(id); });
  $('#cancelBtn').on('click', async () => { await api.post(`/orders/${id}/cancel`); await renderOrderDetail(id); });
}

async function renderAdmin() {
  const tab = (location.hash.split('/')[2] || 'products');
  $('#app').html(`
    <section class="section container layout">
      <aside class="side">
        <a href="#/admin/products">商品管理</a>
        <a href="#/admin/categories">分类管理</a>
        <a href="#/admin/ads">广告管理</a>
        <a href="#/admin/orders">订单管理</a>
        <a href="#/admin/users">用户管理</a>
      </aside>
      <div id="adminPanel"></div>
    </section>
  `);
  if (tab === 'categories') await adminCategories();
  else if (tab === 'ads') await adminAds();
  else if (tab === 'orders') await adminOrders();
  else if (tab === 'users') await adminUsers();
  else await adminProducts();
}

async function adminProducts() {
  categories = await api.get('/admin/categories');
  const rows = await api.get('/admin/products');
  $('#adminPanel').html(`
    <h1>商品管理</h1>
    <form class="form card card-body" id="productForm">${productFormFields()}</form>
    <table class="table"><thead><tr><th>ID</th><th>商品</th><th>价格</th><th>库存</th><th>状态</th><th>操作</th></tr></thead><tbody>${rows.map(p => `<tr><td>${p.id}</td><td>${esc(p.name)}</td><td>${p.price}</td><td>${p.stock}</td><td>${p.status}</td><td><button class="btn btn-light editProduct" data-row='${attr(p)}'>编辑</button> <button class="btn btn-light mediaProduct" data-id="${p.id}">资料</button> <button class="btn btn-danger delProduct" data-id="${p.id}">下架</button></td></tr>`).join('')}</tbody></table>
  `);
  $('#productForm').on('submit', saveProduct);
  $('.editProduct').on('click', function () { fillForm('#productForm', $(this).data('row')); });
  $('.delProduct').on('click', async function () { await api.del(`/admin/products/${$(this).data('id')}`); await adminProducts(); });
  $('.mediaProduct').on('click', function () { mediaModal($(this).data('id')); });
}

function productFormFields() {
  return `
    <input type="hidden" name="id">
    <label>名称<input name="name" required></label>
    <label>副标题<input name="subtitle"></label>
    <label>分类<select name="categoryId">${categories.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('')}</select></label>
    <label>价格<input name="price" type="number" step="0.01" required></label>
    <label>库存<input name="stock" type="number" required></label>
    <label>状态<select name="status"><option value="1">上架</option><option value="0">下架</option></select></label>
    <button class="btn btn-primary">保存商品</button>`;
}

async function saveProduct(e) {
  e.preventDefault();
  const data = formData(e.target);
  data.categoryId = Number(data.categoryId); data.stock = Number(data.stock); data.status = Number(data.status);
  if (data.id) await api.put(`/admin/products/${data.id}`, data); else await api.post('/admin/products', data);
  await adminProducts();
}

async function mediaModal(productId) {
  const rows = await api.get(`/admin/products/${productId}/media`);
  openModal('商品资料', `
    <form class="form" id="mediaForm">
      <label>资料地址<input name="url" required placeholder="http://localhost:9000/mall-media/xxx.jpg"></label>
      <label>类型<select name="mediaType"><option>IMAGE</option><option>VIDEO</option></select></label>
      <label>排序<input name="sortOrder" type="number" value="0"></label>
      <button class="btn btn-primary">添加资料</button>
    </form>
    <table class="table"><tbody>${rows.map(r => `<tr><td>${esc(r.media_type)}</td><td>${esc(r.url)}</td><td><button class="btn btn-danger delMedia" data-id="${r.id}">删除</button></td></tr>`).join('')}</tbody></table>
  `);
  $('#mediaForm').on('submit', async e => { e.preventDefault(); const d = formData(e.target); d.sortOrder = Number(d.sortOrder); await api.post(`/admin/products/${productId}/media`, d); await mediaModal(productId); });
  $('.delMedia').on('click', async function () { await api.del(`/admin/products/${productId}/media/${$(this).data('id')}`); await mediaModal(productId); });
}

async function adminCategories() {
  const rows = await api.get('/admin/categories');
  $('#adminPanel').html(`<h1>分类管理</h1><form class="form card card-body" id="catForm"><input type="hidden" name="id"><label>名称<input name="name" required></label><label>父级ID<input name="parentId" value="0"></label><label>排序<input name="sortOrder" value="0"></label><label>状态<select name="status"><option value="1">启用</option><option value="0">禁用</option></select></label><button class="btn btn-primary">保存分类</button></form><table class="table"><tbody>${rows.map(c => `<tr><td>${c.id}</td><td>${esc(c.name)}</td><td>${c.parent_id}</td><td><button class="btn btn-light editCat" data-row='${attr(c)}'>编辑</button> <button class="btn btn-danger delCat" data-id="${c.id}">禁用</button></td></tr>`).join('')}</tbody></table>`);
  $('#catForm').on('submit', async e => { e.preventDefault(); const d = normalizeCamel(formData(e.target)); d.parentId = Number(d.parentId); d.sortOrder = Number(d.sortOrder); d.status = Number(d.status); if (d.id) await api.put(`/admin/categories/${d.id}`, d); else await api.post('/admin/categories', d); await adminCategories(); });
  $('.editCat').on('click', function () { fillForm('#catForm', normalizeCamel($(this).data('row'))); });
  $('.delCat').on('click', async function () { await api.del(`/admin/categories/${$(this).data('id')}`); await adminCategories(); });
}

async function adminAds() {
  const rows = await api.get('/admin/ads');
  const cats = await api.get('/admin/ads/categories');
  $('#adminPanel').html(`<h1>广告管理</h1><form class="form card card-body" id="adForm"><input type="hidden" name="id"><label>标题<input name="title" required></label><label>广告分类<select name="categoryId">${cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('')}</select></label><label>图片地址<input name="imageUrl" required></label><label>链接<input name="linkUrl"></label><label>排序<input name="sortOrder" value="0"></label><label>状态<select name="status"><option value="1">启用</option><option value="0">禁用</option></select></label><button class="btn btn-primary">保存广告</button></form><table class="table"><tbody>${rows.map(a => `<tr><td>${a.id}</td><td>${esc(a.title)}</td><td>${esc(a.category_name || '')}</td><td><button class="btn btn-light editAd" data-row='${attr(a)}'>编辑</button> <button class="btn btn-danger delAd" data-id="${a.id}">禁用</button></td></tr>`).join('')}</tbody></table>`);
  $('#adForm').on('submit', async e => { e.preventDefault(); const d = normalizeCamel(formData(e.target)); d.categoryId = Number(d.categoryId); d.sortOrder = Number(d.sortOrder); d.status = Number(d.status); if (d.id) await api.put(`/admin/ads/${d.id}`, d); else await api.post('/admin/ads', d); await adminAds(); });
  $('.editAd').on('click', function () { fillForm('#adForm', normalizeCamel($(this).data('row'))); });
  $('.delAd').on('click', async function () { await api.del(`/admin/ads/${$(this).data('id')}`); await adminAds(); });
}

async function adminOrders() {
  const rows = await api.get('/admin/orders');
  $('#adminPanel').html(`<h1>订单管理</h1>${orderTable(rows, true)}`);
  $('.adminStatus').on('change', async function () { await api.put(`/admin/orders/${$(this).data('id')}/status`, { status: Number($(this).val()) }); });
}

async function adminUsers() {
  const rows = await api.get('/admin/users');
  $('#adminPanel').html(`<h1>用户管理</h1><table class="table"><thead><tr><th>ID</th><th>用户名</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody>${rows.map(u => `<tr><td>${u.id}</td><td>${esc(u.username)}</td><td>${esc(u.role)}</td><td>${u.status}</td><td><button class="btn btn-light userStatus" data-id="${u.id}" data-status="${u.status == 1 ? 0 : 1}">${u.status == 1 ? '禁用' : '启用'}</button></td></tr>`).join('')}</tbody></table>`);
  $('.userStatus').on('click', async function () { await api.put(`/admin/users/${$(this).data('id')}/status`, { status: Number($(this).data('status')) }); await adminUsers(); });
}

function loginModal() {
  openModal('用户登录', `<form class="form" id="loginForm"><label>用户名<input name="username" value="admin" required></label><label>密码<input name="password" type="password" value="admin123" required></label><button class="btn btn-primary">登录</button><p class="muted">默认管理员：admin / admin123</p></form><p><button class="btn btn-light" id="showRegister">注册新用户</button></p>`);
  $('#loginForm').on('submit', async e => { e.preventDefault(); currentUser = await api.post('/users/login', formData(e.target)); closeModal(); refreshSessionUi(); await refreshCartBadge(); route(); });
  $('#showRegister').on('click', registerModal);
}

function registerModal() {
  openModal('用户注册', `<form class="form" id="registerForm"><label>用户名<input name="username" required></label><label>密码<input name="password" type="password" required></label><label>手机号<input name="phone"></label><label>邮箱<input name="email"></label><label>验证码<input name="code" value="123456"></label><button class="btn btn-primary">注册</button></form>`);
  $('#registerForm').on('submit', async e => { e.preventDefault(); await api.post('/users/register', formData(e.target)); alert('注册成功，请登录'); loginModal(); });
}

async function requireLogin(fn) {
  if (!currentUser) {
    loginModal();
    throw new Error('请先登录');
  }
  return fn();
}

async function requireAdmin(fn) {
  await requireLogin(async () => {});
  if (currentUser.role !== 'ADMIN') {
    alert('需要管理员权限');
    location.hash = '#/';
    return;
  }
  return fn();
}

async function refreshCartBadge() {
  if (!currentUser) { $('#cartBadge').text(0); return; }
  try { $('#cartBadge').text((await api.get('/cart')).length); } catch { $('#cartBadge').text(0); }
}

function productCard(p) {
  return `<a class="card" href="#/products/${p.id}"><img class="product-img" src="${esc(p.cover || 'https://dummyimage.com/600x360/f5f5f7/86868b&text=Product')}"><div class="card-body"><h3 class="product-title">${esc(p.name)}</h3><p class="muted">${esc(p.subtitle || '')}</p><p class="price">￥${p.price}</p></div></a>`;
}

function adCard(a) {
  return `<a class="card" href="${esc(a.link_url || '#/products')}"><img class="ad-img" src="${esc(a.image_url)}"><div class="card-body"><strong>${esc(a.title)}</strong></div></a>`;
}

function orderTable(rows, admin) {
  return `<table class="table"><thead><tr><th>订单号</th><th>用户</th><th>金额</th><th>状态</th><th>收货信息</th><th>操作</th></tr></thead><tbody>${rows.map(o => `<tr><td>${esc(o.order_no)}</td><td>${esc(o.username || '')}</td><td>￥${o.total_amount}</td><td>${admin ? `<select class="adminStatus" data-id="${o.id}">${[0,1,2,3,4].map(s => `<option value="${s}" ${o.status == s ? 'selected' : ''}>${statusText(s)}</option>`).join('')}</select>` : statusText(o.status)}</td><td>${esc(o.receiver_name)} ${esc(o.receiver_phone)}<br>${esc(o.receiver_address)}</td><td><a class="btn btn-light" href="#/orders/${o.id}">详情</a></td></tr>`).join('') || '<tr><td colspan="6">暂无订单</td></tr>'}</tbody></table>`;
}

function statusText(status) {
  return ['待支付', '已支付', '已发货', '已完成', '已取消'][Number(status)] || '未知';
}

function firstMedia(p) {
  return p.media && p.media.length ? p.media[0].url : p.cover;
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function normalizeCamel(row) {
  const copy = { ...row };
  if ('category_id' in copy) copy.categoryId = copy.category_id;
  if ('image_url' in copy) copy.imageUrl = copy.image_url;
  if ('link_url' in copy) copy.linkUrl = copy.link_url;
  if ('sort_order' in copy) copy.sortOrder = copy.sort_order;
  if ('parent_id' in copy) copy.parentId = copy.parent_id;
  return copy;
}

function fillForm(selector, row) {
  Object.entries(row).forEach(([key, value]) => $(`${selector} [name="${key}"]`).val(value));
}

function openModal(title, body) {
  $('#modalTitle').text(title);
  $('#modalBody').html(body);
  $('#modal').removeClass('hidden');
}

function closeModal() {
  $('#modal').addClass('hidden');
}

function showError(error) {
  if (String(error.message || error).includes('请先登录')) return;
  $('#app').html(`<section class="section container"><div class="notice">加载失败：${esc(error.responseJSON?.message || error.message || error)}</div></section>`);
}

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, s => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]));
}

function attr(row) {
  return esc(JSON.stringify(normalizeCamel(row)));
}

function empty(text) {
  return `<div class="notice">${text}</div>`;
}
