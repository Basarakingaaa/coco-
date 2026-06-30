import { useEffect, useRef, useState } from 'react';
import { adminApi } from '../../api';

const EMPTY_FORM = {
  categoryId: '',
  name: '',
  subtitle: '',
  price: '',
  stock: 0,
  status: 1
};

function ProductMediaManager({ productId }) {
  const [media, setMedia] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const imageInputRef = useRef(null);
  const videoInputRef = useRef(null);

  const load = async () => {
    setLoading(true);
    const res = await adminApi.listProductMedia(productId);
    setMedia(res.data || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, [productId]);

  const handleAdd = async (e, mediaType) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const uploadRes = await adminApi.upload(file);
      await adminApi.addProductMedia(productId, { mediaType, url: uploadRes.data.url });
      await load();
    } catch (err) {
      setError(err.message || '上传失败');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const handleDelete = async (mediaId) => {
    await adminApi.deleteProductMedia(productId, mediaId);
    await load();
  };

  return (
    <div className="form-field">
      <label>商品资料（图片 / 视频，可添加多条，与上架状态互不影响）</label>

      {loading ? (
        <p style={{ fontSize: 13, color: '#86868b' }}>加载中...</p>
      ) : media.length === 0 ? (
        <p style={{ fontSize: 13, color: '#86868b' }}>暂无资料</p>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
          {media.map((m) => (
            <div key={m.id} className="upload-preview" style={{ position: 'relative' }}>
              {m.mediaType === 'VIDEO' ? <video src={m.url} muted /> : <img src={m.url} alt="" />}
              <button
                type="button"
                onClick={() => handleDelete(m.id)}
                style={{
                  position: 'absolute', top: 2, right: 2, width: 20, height: 20,
                  borderRadius: '50%', background: 'rgba(0,0,0,0.6)', color: '#fff', fontSize: 12
                }}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      <div style={{ display: 'flex', gap: 10 }}>
        <button type="button" className="btn btn-secondary" onClick={() => imageInputRef.current?.click()} disabled={uploading}>
          {uploading ? '上传中...' : '+ 添加图片'}
        </button>
        <button type="button" className="btn btn-secondary" onClick={() => videoInputRef.current?.click()} disabled={uploading}>
          {uploading ? '上传中...' : '+ 添加视频'}
        </button>
        <input ref={imageInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={(e) => handleAdd(e, 'IMAGE')} />
        <input ref={videoInputRef} type="file" accept="video/*" style={{ display: 'none' }} onChange={(e) => handleAdd(e, 'VIDEO')} />
      </div>
      {error && <p className="form-error">{error}</p>}
    </div>
  );
}

export default function AdminProducts() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // null=未打开, {}=新增, {id,...}=编辑
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    const [productRes, categoryRes] = await Promise.all([
      adminApi.listProducts({ page: 1, size: 50 }),
      adminApi.listCategories()
    ]);
    setProducts(productRes.data?.content || []);
    setCategories(categoryRes.data || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setError('');
    setEditing({});
  };

  const openEdit = (p) => {
    setForm({
      categoryId: p.categoryId,
      name: p.name,
      subtitle: p.subtitle || '',
      price: p.price,
      stock: p.stock,
      status: p.status
    });
    setError('');
    setEditing(p);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = { ...form, categoryId: Number(form.categoryId), price: Number(form.price), stock: Number(form.stock) };
      if (editing?.id) {
        await adminApi.updateProduct(editing.id, payload);
        await load();
      } else {
        const res = await adminApi.createProduct(payload);
        await load();
        // 创建成功后留在编辑态，方便立即补充图片/视频资料
        setEditing(res.data);
      }
    } catch (err) {
      setError(err.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('确定删除该商品？关联的资料也会一并删除。')) return;
    await adminApi.deleteProduct(id);
    await load();
  };

  const categoryName = (id) => categories.find((c) => c.id === id)?.name || '-';

  return (
    <div>
      <div className="admin-header">
        <h1 className="admin-title">商品管理</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ 新增商品</button>
      </div>

      {loading ? (
        <p>加载中...</p>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>封面</th>
              <th>名称</th>
              <th>分类</th>
              <th>价格</th>
              <th>库存</th>
              <th>上架状态</th>
              <th>资料状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>
                  {p.coverImage ? <img src={p.coverImage} alt={p.name} /> : <span style={{ fontSize: 11, color: '#86868b' }}>无</span>}
                </td>
                <td>{p.name}</td>
                <td>{categoryName(p.categoryId)}</td>
                <td>¥{p.price}</td>
                <td>{p.stock}</td>
                <td>
                  <span className={`admin-badge ${p.status === 1 ? 'on' : 'off'}`}>
                    {p.status === 1 ? '上架' : '下架'}
                  </span>
                </td>
                <td>
                  <span className={`admin-badge ${p.hasMedia ? 'on' : 'off'}`}>
                    {p.hasMedia ? '有资料' : '无资料'}
                  </span>
                </td>
                <td>
                  <button className="admin-link-btn" onClick={() => openEdit(p)}>编辑</button>
                  <button className="admin-link-btn danger" onClick={() => handleDelete(p.id)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="admin-modal-overlay" onClick={() => setEditing(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="form-title">{editing?.id ? '编辑商品' : '新增商品'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-field">
                <label>分类</label>
                <select
                  value={form.categoryId}
                  onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                  required
                  style={{ width: '100%', padding: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
                >
                  <option value="" disabled>请选择分类</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label>商品名称</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </div>
              <div className="form-field">
                <label>副标题</label>
                <input value={form.subtitle} onChange={(e) => setForm({ ...form, subtitle: e.target.value })} />
              </div>
              <div className="form-field">
                <label>价格</label>
                <input type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
              </div>
              <div className="form-field">
                <label>库存</label>
                <input type="number" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} required />
              </div>
              <div className="form-field">
                <label>上架状态</label>
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
                  style={{ width: '100%', padding: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
                >
                  <option value={1}>上架</option>
                  <option value={0}>下架</option>
                </select>
              </div>
              {error && <p className="form-error">{error}</p>}
              <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>关闭</button>
                <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                  {saving ? '保存中...' : '保存基础信息'}
                </button>
              </div>
            </form>

            {editing?.id && (
              <>
                <hr style={{ margin: '24px 0', border: 'none', borderTop: '1px solid var(--color-border)' }} />
                <ProductMediaManager productId={editing.id} />
              </>
            )}
            {!editing?.id && (
              <p style={{ fontSize: 13, color: '#86868b', marginTop: 16 }}>
                保存基础信息后才能添加图片/视频资料。
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
