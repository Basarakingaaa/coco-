import { useEffect, useState } from 'react';
import { adminApi } from '../../api';

const EMPTY_FORM = { name: '', parentId: 0, sortOrder: 0, status: 1 };

export default function AdminCategories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    const res = await adminApi.listCategories();
    setCategories(res.data || []);
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

  const openEdit = (c) => {
    setForm({ name: c.name, parentId: c.parentId, sortOrder: c.sortOrder, status: c.status });
    setError('');
    setEditing(c);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = { ...form, parentId: Number(form.parentId), sortOrder: Number(form.sortOrder) };
      if (editing?.id) {
        await adminApi.updateCategory(editing.id, payload);
      } else {
        await adminApi.createCategory(payload);
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
    if (!window.confirm('确定删除该分类？')) return;
    await adminApi.deleteCategory(id);
    await load();
  };

  return (
    <div>
      <div className="admin-header">
        <h1 className="admin-title">分类管理</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ 新增分类</button>
      </div>

      {loading ? (
        <p>加载中...</p>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{c.sortOrder}</td>
                <td>
                  <span className={`admin-badge ${c.status === 1 ? 'on' : 'off'}`}>
                    {c.status === 1 ? '启用' : '停用'}
                  </span>
                </td>
                <td>
                  <button className="admin-link-btn" onClick={() => openEdit(c)}>编辑</button>
                  <button className="admin-link-btn danger" onClick={() => handleDelete(c.id)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <div className="admin-modal-overlay" onClick={() => setEditing(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="form-title">{editing?.id ? '编辑分类' : '新增分类'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-field">
                <label>名称</label>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </div>
              <div className="form-field">
                <label>排序（数字越小越靠前）</label>
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
