import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { cartApi, productApi } from '../api';
import { useCart } from '../context/CartContext.jsx';

export default function Cart() {
  const navigate = useNavigate();
  const { refresh } = useCart();
  const [items, setItems] = useState([]);
  const [productInfo, setProductInfo] = useState({});
  const [selected, setSelected] = useState(new Set());
  const [loading, setLoading] = useState(true);

  const loadCart = async () => {
    if (!localStorage.getItem('token')) {
      setItems([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const res = await cartApi.list();
      const cartItems = res.data || [];
      setItems(cartItems);

      const productIds = [...new Set(cartItems.map((i) => i.productId))];
      const infoMap = {};
      await Promise.all(
        productIds.map(async (pid) => {
          const detail = await productApi.detail(pid);
          infoMap[pid] = detail.data;
        })
      );
      setProductInfo(infoMap);
      setSelected(new Set(cartItems.map((i) => i.id)));
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCart();
  }, []);

  const enriched = useMemo(() => {
    return items.map((item) => {
      const info = productInfo[item.productId];
      const product = info?.product;
      const sku = info?.skuList?.find((s) => s.id === item.skuId);
      const coverImage = info?.media?.find((m) => m.mediaType === 'IMAGE')?.url;
      return {
        ...item,
        name: product?.name || '商品',
        image: sku?.image || coverImage,
        spec: sku?.specDesc,
        price: sku?.price ?? product?.price ?? 0
      };
    });
  }, [items, productInfo]);

  const total = enriched
    .filter((i) => selected.has(i.id))
    .reduce((sum, i) => sum + i.price * i.quantity, 0);

  const toggleSelect = (id) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const changeQuantity = async (id, quantity) => {
    if (quantity < 1) return;
    await cartApi.updateQuantity(id, quantity);
    await loadCart();
    await refresh();
  };

  const removeItem = async (id) => {
    await cartApi.remove([id]);
    await loadCart();
    await refresh();
  };

  const handleCheckout = () => {
    navigate('/checkout', { state: { cartItemIds: [...selected] } });
  };

  if (loading) {
    return <div className="empty-state">加载中...</div>;
  }

  if (items.length === 0) {
    return (
      <div className="empty-state">
        <p>购物车空空如也</p>
        <Link to="/products" className="btn btn-secondary">去逛逛</Link>
      </div>
    );
  }

  return (
    <div className="container section">
      <h1 className="section-title">购物车</h1>

      <div>
        {enriched.map((item) => (
          <div key={item.id} className="cart-item">
            <input
              type="checkbox"
              checked={selected.has(item.id)}
              onChange={() => toggleSelect(item.id)}
            />
            <div className="cart-item-image">
              {item.image ? <img src={item.image} alt={item.name} /> : <span className="no-media-text">无图</span>}
            </div>
            <div className="cart-item-info">
              <p className="cart-item-name">{item.name}</p>
              {item.spec && <p className="cart-item-spec">{item.spec}</p>}
              <div className="qty-control">
                <button className="qty-btn" onClick={() => changeQuantity(item.id, item.quantity - 1)}>−</button>
                <span>{item.quantity}</span>
                <button className="qty-btn" onClick={() => changeQuantity(item.id, item.quantity + 1)}>+</button>
                <button className="btn-secondary" onClick={() => removeItem(item.id)}>删除</button>
              </div>
            </div>
            <div className="cart-item-price">¥{(item.price * item.quantity).toFixed(2)}</div>
          </div>
        ))}
      </div>

      <div className="cart-summary">
        <span className="cart-total">合计：¥{total.toFixed(2)}</span>
        <button
          className="btn btn-primary"
          disabled={selected.size === 0}
          onClick={handleCheckout}
        >
          结算 ({selected.size})
        </button>
      </div>
    </div>
  );
}
