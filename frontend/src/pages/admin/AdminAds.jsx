import { useEffect, useState } from 'react';
import { adminApi } from '../../api';
import MediaUpload from '../../components/MediaUpload.jsx';

const EMPTY_FORM = { categoryId: '', title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 1 };

export default function AdminAds() {
  const [ads, setAds] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');

  const load = async () => {
    setLoading(true);
    const [adRes, catRes] = await Promise.all([adminApi.listAds(), adminApi.listAdCategories()]);
    setAds(adRes.data || []);
    setCategories(catRes.data || []);
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

  const openEdit = (ad) => {
    setForm({
      categoryId: ad.categoryId,
      title: ad.title,
      imageUrl: ad.imageUrl,
      linkUrl: ad.linkUrl || '',
      sortOrder: ad.sortOrder,
      status: ad.status
    });
    setError('');
    setEditing(ad);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = { ...form, categoryId: Number(form.categoryId), sortOrder: Number(form.sortOrder) };
      if (editing?.id) {
        await adminApi.updateAd(editing.id, payload);
      } else {
        await adminApi.createAd(payload);
      }
      setEditing(null);
      await load();
    } catch (err) {
      setError(err.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('确定删除该广告？')) return;
    await adminApi.deleteAd(id);
    await load();
  };

  const handleAddCategory = async () => {
    if (!newCategoryName.trim()) return;
    await adminApi.createAdCategory(newCategoryName.trim());
    setNewCategoryName('');
    await load();
  };

  const categoryName = (id) => categories.find((c) => c.id === id)?.name || '-';

  return (
    <div>
      <div className="admin-header">
        <h1 className="admin-title">广告管理</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ 新增广告</button>
      </div>

      <div style={{ display: 'flex', gap: 10, marginBottom: 24 }}>
        <input
          className="search-input"
          placeholder="新建广告类别名称"
          value={newCategoryName}
          onChange={(e) => setNewCategoryName(e.target.value)}
        />
        <button className="btn btn-secondary" onClick={handleAddCategory}>添加类别</button>
      </div>

      {loading ? (
        <p>加载中...</p>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>图片</th>
              <th>标题</th>
              <th>类别</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {ads.map((ad) => (
              <tr key={ad.id}>
                <td><img src={ad.imageUrl} alt={ad.title} /></td>
                <td>{ad.title}</td>
                <td>{categoryName(ad.categoryId)}</td>
                <td>{ad.sortOrder}</td>
                <td>
                  <span className={`admin-badge ${ad.status === 1 ? 'on' : 'off'}`}>
                    {ad.status === 1 ? '启用' : '停用'}
                  </span>
                </td>
                <td>
                  <button className="admin-link-btn" onClick={() => openEdit(ad)}>编辑</button>
                  <button className="admin-link-btn danger" onClick={() => handleDelete(ad.id)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="admin-modal-overlay" onClick={() => setEditing(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="form-title">{editing?.id ? '编辑广告' : '新增广告'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-field">
                <label>类别</label>
                <select
                  value={form.categoryId}
                  onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                  required
                  style={{ width: '100%', padding: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
                >
                  <option value="" disabled>请选择类别</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label>标题</label>
                <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              </div>
              <MediaUpload
                label="广告图"
                value={form.imageUrl}
                onChange={(url) => setForm({ ...form, imageUrl: url })}
                accept="image/*"
              />
              <div className="form-field">
                <label>跳转链接（可选）</label>
                <input value={form.linkUrl} onChange={(e) => setForm({ ...form, linkUrl: e.target.value })} />
              </div>
              <div className="form-field">
                <label>排序</label>
                <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
              </div>
              <div className="form-field">
                <label>状态</label>
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: Number(e.target.value) })}
                  style={{ width: '100%', padding: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
                >
                  <option value={1}>启用</option>
                  <option value={0}>停用</option>
                </select>
              </div>
              {error && <p className="form-error">{error}</p>}
              <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>取消</button>
                <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                  {saving ? '保存中...' : '保存'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
