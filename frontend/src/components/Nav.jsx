import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext.jsx';
import { getCurrentRole } from '../utils/jwt.js';

export default function Nav() {
  const navigate = useNavigate();
  const { count } = useCart();
  const isLoggedIn = !!localStorage.getItem('token');
  const isAdmin = getCurrentRole() === 'ADMIN';

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/');
    window.location.reload();
  };

  return (
    <nav className="nav">
      <div className="nav-inner">
        <Link to="/" className="nav-logo">商城</Link>
        <div className="nav-links">
          <Link to="/products">全部商品</Link>
          <Link to="/cart">
            购物车
            {count > 0 && <span className="nav-cart-badge">{count}</span>}
          </Link>
          {isAdmin && <Link to="/admin">后台管理</Link>}
          {isLoggedIn ? (
            <button onClick={handleLogout}>退出</button>
          ) : (
            <Link to="/login">登录</Link>
          )}
        </div>
      </div>
    </nav>
  );
}
