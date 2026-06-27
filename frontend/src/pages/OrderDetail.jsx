import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { orderApi } from '../api';

const STATUS_LABEL = ['待支付', '已支付', '已发货', '已完成', '已取消'];

export default function OrderDetail() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState('');

  const loadOrder = () => {
    orderApi
      .detail(id)
      .then((res) => setOrder(res.data))
      .catch((e) => setLoadError(e.message || '订单加载失败'));
  };

  useEffect(() => {
    loadOrder();
  }, [id]);

  const handlePay = async () => {
    setPaying(true);
    setPayError('');
    try {
      await orderApi.pay(id);
      loadOrder();
    } catch (e) {
      setPayError(e.message || '支付失败');
    } finally {
      setPaying(false);
    }
  };

  if (loadError) {
    return <div className="empty-state">{loadError}</div>;
  }

  if (!order) {
    return <div className="empty-state">加载中...</div>;
  }

  const isPending = order.status === 0;

  return (
    <div className="form-card">
      <h1 className="form-title">订单详情</h1>
      <div className="spec-table">
        <div className="spec-row">
          <div className="spec-row-key">订单号</div>
          <div>{order.orderNo}</div>
        </div>
        <div className="spec-row">
          <div className="spec-row-key">状态</div>
          <div>{STATUS_LABEL[order.status]}</div>
        </div>
        <div className="spec-row">
          <div className="spec-row-key">金额</div>
          <div>¥{order.totalAmount}</div>
        </div>
        <div className="spec-row">
          <div className="spec-row-key">收货人</div>
          <div>{order.receiverName} {order.receiverPhone}</div>
        </div>
        <div className="spec-row">
          <div className="spec-row-key">地址</div>
          <div>{order.receiverAddress}</div>
        </div>
      </div>

      {payError && <p className="form-error">{payError}</p>}

      {isPending ? (
        <button
          className="btn btn-primary btn-block"
          style={{ marginTop: 24 }}
          onClick={handlePay}
          disabled={paying}
        >
          {paying ? '支付中...' : `立即支付 ¥${order.totalAmount}`}
        </button>
      ) : (
        <Link to="/products" className="btn btn-primary btn-block" style={{ marginTop: 24 }}>
          继续购物
        </Link>
      )}
    </div>
  );
}
