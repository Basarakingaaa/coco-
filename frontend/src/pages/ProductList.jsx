import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { categoryApi, productApi } from '../api';

export default function ProductList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryId = searchParams.get('categoryId') || '';
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    categoryApi.list().then((res) => setCategories(res.data || []));
  }, []);

  useEffect(() => {
    setLoading(true);
    productApi
      .list({ categoryId: categoryId || undefined, keyword: keyword || undefined, page: 1, size: 24 })
      .then((res) => setProducts(res.data?.content || []))
      .finally(() => setLoading(false));
  }, [categoryId, keyword]);

  const selectCategory = (id) => {
    if (id) {
      setSearchParams({ categoryId: id });
    } else {
      setSearchParams({});
    }
  };

  return (
    <div className="container section">
      <h1 className="section-title">全部商品</h1>

      <div className="toolbar">
        <div className="filter-pills">
          <button
            className={`filter-pill ${!categoryId ? 'active' : ''}`}
            onClick={() => selectCategory('')}
          >
            全部
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`filter-pill ${categoryId === String(cat.id) ? 'active' : ''}`}
              onClick={() => selectCategory(cat.id)}
            >
              {cat.name}
            </button>
          ))}
        </div>
        <input
          className="search-input"
          placeholder="搜索商品"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="empty-state">加载中...</div>
      ) : products.length === 0 ? (
        <div className="empty-state">暂无商品</div>
      ) : (
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
      )}
    </div>
  );
}
