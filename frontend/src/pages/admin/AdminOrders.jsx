import { useEffect, useState } from 'react';
import { adminApi } from '../../api';

const STATUS_LABELS = ['待支付', '已支付', '已发货', '已完成', '已取消'];

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const res = await adminApi.listOrders({ page: 1, size: 50 });
    setOrders(res.data?.content || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const handleStatusChange = async (id, status) => {
    await adminApi.updateOrderStatus(id, Number(status));
    await load();
  };

  return (
    <div>
      <div className="admin-header">
        <h1 className="admin-title">订单管理</h1>
      </div>

      {loading ? (
        <p>加载中...</p>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>收货人</th>
              <th>金额</th>
              <th>状态</th>
              <th>下单时间</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.orderNo}</td>
                <td>{o.receiverName} {o.receiverPhone}</td>
                <td>¥{o.totalAmount}</td>
                <td>
                  <select
                    value={o.status}
                    onChange={(e) => handleStatusChange(o.id, e.target.value)}
                    style={{ padding: 6, borderRadius: 8, border: '1px solid var(--color-border)' }}
                  >
                    {STATUS_LABELS.map((label, idx) => (
                      <option key={idx} value={idx}>{label}</option>
                    ))}
                  </select>
                </td>
                <td>{o.createdAt?.replace('T', ' ')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
