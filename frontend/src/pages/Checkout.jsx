import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { orderApi } from '../api';
import { useCart } from '../context/CartContext.jsx';

export default function Checkout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { refresh } = useCart();
  const cartItemIds = location.state?.cartItemIds || [];

  const [form, setForm] = useState({ receiverName: '', receiverPhone: '', receiverAddress: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (cartItemIds.length === 0) {
    return (
      <div className="empty-state">
        <p>没有待结算的商品</p>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await orderApi.checkout({ cartItemIds, ...form });
      await refresh();
      navigate(`/orders/${res.data.id}`);
    } catch (e) {
      setError(e.message || '提交订单失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h1 className="form-title">确认订单</h1>
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label>收货人</label>
          <input
            value={form.receiverName}
            onChange={(e) => setForm({ ...form, receiverName: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label>联系电话</label>
          <input
            value={form.receiverPhone}
            onChange={(e) => setForm({ ...form, receiverPhone: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label>收货地址</label>
          <textarea
            rows={3}
            value={form.receiverAddress}
            onChange={(e) => setForm({ ...form, receiverAddress: e.target.value })}
            required
          />
        </div>
        {error && <p className="form-error">{error}</p>}
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? '提交中...' : '提交订单'}
        </button>
      </form>
    </div>
  );
}
