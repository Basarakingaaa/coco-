import axios from 'axios';

const client = axios.create({
  baseURL: '/api'
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
    }
    return Promise.reject(error.response?.data || error);
  }
);

export const productApi = {
  list: (params) => client.get('/products', { params }),
  detail: (id) => client.get(`/products/${id}`)
};

export const categoryApi = {
  list: (parentId) => client.get('/categories', { params: { parentId } })
};

export const adApi = {
  list: (categoryId) => client.get('/ads', { params: { categoryId } })
};

export const userApi = {
  login: (data) => client.post('/users/login', data),
  register: (data) => client.post('/users/register', data),
  sendCode: (email) => client.post('/users/send-code', { email })
};

export const cartApi = {
  list: () => client.get('/cart'),
  add: (data) => client.post('/cart', data),
  updateQuantity: (id, quantity) => client.put(`/cart/${id}`, { quantity }),
  remove: (cartItemIds) => client.delete('/cart', { data: cartItemIds })
};

export const orderApi = {
  checkout: (data) => client.post('/orders/checkout', data),
  list: (params) => client.get('/orders', { params }),
  detail: (id) => client.get(`/orders/${id}`),
  pay: (id) => client.post(`/orders/${id}/pay`),
  cancel: (id) => client.post(`/orders/${id}/cancel`)
};

export const adminApi = {
  // 文件上传
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return client.post('/admin/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  deleteFile: (url) => client.delete('/admin/files', { params: { url } }),

  // 商品
  listProducts: (params) => client.get('/admin/products', { params }),
  getProduct: (id) => client.get(`/admin/products/${id}`),
  createProduct: (data) => client.post('/admin/products', data),
  updateProduct: (id, data) => client.put(`/admin/products/${id}`, data),
  deleteProduct: (id) => client.delete(`/admin/products/${id}`),

  // 商品资料（图片/视频），与商品上下架状态解耦
  listProductMedia: (productId) => client.get(`/admin/products/${productId}/media`),
  addProductMedia: (productId, data) => client.post(`/admin/products/${productId}/media`, data),
  deleteProductMedia: (productId, mediaId) => client.delete(`/admin/products/${productId}/media/${mediaId}`),

  // 分类
  listCategories: () => client.get('/admin/categories'),
  createCategory: (data) => client.post('/admin/categories', data),
  updateCategory: (id, data) => client.put(`/admin/categories/${id}`, data),
  deleteCategory: (id) => client.delete(`/admin/categories/${id}`),

  // 广告
  listAds: () => client.get('/admin/ads'),
  createAd: (data) => client.post('/admin/ads', data),
  updateAd: (id, data) => client.put(`/admin/ads/${id}`, data),
  deleteAd: (id) => client.delete(`/admin/ads/${id}`),
  listAdCategories: () => client.get('/admin/ads/categories'),
  createAdCategory: (name) => client.post('/admin/ads/categories', { name }),

  // 订单
  listOrders: (params) => client.get('/admin/orders', { params }),
  updateOrderStatus: (id, status) => client.put(`/admin/orders/${id}/status`, { status }),

  // 用户
  listUsers: (params) => client.get('/admin/users', { params }),
  updateUserStatus: (id, status) => client.put(`/admin/users/${id}/status`, { status })
};

export default client;
