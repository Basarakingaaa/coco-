import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adApi, categoryApi, productApi } from '../api';

export default function Home() {
  const [ads, setAds] = useState([]);
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);

  useEffect(() => {
    adApi.list().then((res) => setAds(res.data || []));
    categoryApi.list().then((res) => setCategories(res.data || []));
    productApi.list({ page: 1, size: 8 }).then((res) => setProducts(res.data?.content || []));
  }, []);

  const hero = ads[0];

  return (
    <div>
      <section className="hero">
        <p className="hero-eyebrow">新品发布</p>
        <h1 className="hero-title">{hero?.title || '发现非凡好物'}</h1>
        <p className="hero-subtitle">精选品质生活，从这里开始。</p>
        <Link to="/products" className="btn btn-primary">立即选购</Link>
        {hero?.imageUrl && (
          <div className="hero-image">
            <img src={hero.imageUrl} alt={hero.title} />
          </div>
        )}
      </section>

      <section className="section container">
        <h2 className="section-title">分类浏览</h2>
        <div className="category-grid">
          {categories.map((cat) => (
            <Link key={cat.id} to={`/products?categoryId=${cat.id}`} className="category-card">
              <div className="category-card-icon">{cat.icon || '🛍️'}</div>
              <div className="category-card-name">{cat.name}</div>
            </Link>
          ))}
        </div>
      </section>

      <section className="section container">
        <h2 className="section-title">为你推荐</h2>
        <div className="product-grid">
          {products.map((p) => (
            <Link key={p.id} to={`/products/${p.id}`} className="product-card">
              <div className="product-card-image">
                {p.coverImage ? <img src={p.coverImage} alt={p.name} /> : <span className="no-media-text">暂无图片</span>}
              </div>
              <h3 className="product-card-name">{p.name}</h3>
              <p className="product-card-subtitle">{p.subtitle}</p>
              <p className="product-card-price">¥{p.price}</p>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
