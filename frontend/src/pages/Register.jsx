import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { userApi } from '../api';
import { useCart } from '../context/CartContext.jsx';

export default function Register() {
  const navigate = useNavigate();
  const { refresh } = useCart();
  const [form, setForm] = useState({ username: '', password: '', phone: '', email: '', code: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const timerRef = useRef(null);

  useEffect(() => () => clearInterval(timerRef.current), []);

  const startCooldown = () => {
    setCooldown(60);
    timerRef.current = setInterval(() => {
      setCooldown((c) => {
        if (c <= 1) {
          clearInterval(timerRef.current);
          return 0;
        }
        return c - 1;
      });
    }, 1000);
  };

  const handleSendCode = async () => {
    if (!form.email) {
      setError('请先填写邮箱');
      return;
    }
    setError('');
    setSending(true);
    try {
      await userApi.sendCode(form.email);
      startCooldown();
    } catch (e) {
      setError(e.message || '验证码发送失败');
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await userApi.register(form);
      localStorage.setItem('token', res.data.token);
      await refresh();
      navigate('/');
    } catch (e) {
      setError(e.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h1 className="form-title">注册</h1>
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
        <div className="form-field">
          <label>手机号</label>
          <input
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
          />
        </div>
        <div className="form-field">
          <label>邮箱</label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label>验证码</label>
          <div style={{ display: 'flex', gap: 10 }}>
            <input
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              maxLength={6}
              required
              style={{ flex: 1 }}
            />
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleSendCode}
              disabled={sending || cooldown > 0}
            >
              {cooldown > 0 ? `${cooldown}s` : sending ? '发送中...' : '获取验证码'}
            </button>
          </div>
        </div>
        {error && <p className="form-error">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? '注册中...' : '注册'}
        </button>
      </form>
      <p className="form-footer">
        已有账号？ <Link to="/login" className="btn-secondary">立即登录</Link>
      </p>
    </div>
  );
}
