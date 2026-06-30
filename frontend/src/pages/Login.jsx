import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi } from '../api';
import { useCart } from '../context/CartContext.jsx';
import { getCurrentRole } from '../utils/jwt.js';

export default function Login() {
  const navigate = useNavigate();
  const { refresh } = useCart();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await userApi.login(form);
      localStorage.setItem('token', res.data.token);
      await refresh();
      navigate(getCurrentRole() === 'ADMIN' ? '/admin' : '/');
    } catch (e) {
      setError(e.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h1 className="form-title">登录</h1>
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label>用户名</label>
          <input
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label>密码</label>
          <input
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </div>
        {error && <p className="form-error">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? '登录中...' : '登录'}
        </button>
      </form>
      <p className="form-footer">
        还没有账号？ <Link to="/register" className="btn-secondary">立即注册</Link>
      </p>
    </div>
  );
}
