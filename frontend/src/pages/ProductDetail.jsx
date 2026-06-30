import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { cartApi, productApi } from '../api';
import { useCart } from '../context/CartContext.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { refresh } = useCart();

  const [detail, setDetail] = useState(null);
  const [selectedSku, setSelectedSku] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [message, setMessage] = useState('');
  const [showVideo, setShowVideo] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    setLoading(true);
    setLoadError('');
    productApi
      .detail(id)
      .then((res) => {
        setDetail(res.data);
        const skuList = res.data?.skuList || [];
        setSelectedSku(skuList.length > 0 ? skuList[0] : null);
      })
      .catch((e) => {
        setLoadError(e.message || '商品加载失败');
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <div className="empty-state">加载中...</div>;
  }

  if (loadError || !detail) {
    return <div className="empty-state">{loadError || '商品不存在'}</div>;
  }

  const { product, info1 = [], info2 = [], skuList = [], media = [] } = detail;
  const coverImage = media.find((m) => m.mediaType === 'IMAGE')?.url;
  const video = media.find((m) => m.mediaType === 'VIDEO')?.url;
  const image = selectedSku?.image || info1[0]?.imageUrl || coverImage;
  const price = selectedSku?.price ?? product.price;
  const hasVideo = !!video;
  const hasAnyMedia = !!image || hasVideo;

  const handleAddToCart = async () => {
    if (!localStorage.getItem('token')) {
      navigate('/login');
      return;
    }
    setAdding(true);
    setMessage('');
    try {
      await cartApi.add({ productId: product.id, skuId: selectedSku?.id, quantity });
      await refresh();
      setMessage('已加入购物车');
    } catch (e) {
      setMessage(e.message || '添加失败');
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="container">
      <div className="detail-layout">
        <div className="detail-image">
          {hasVideo && showVideo ? (
            <video src={video} poster={image} controls autoPlay muted loop />
          ) : hasAnyMedia ? (
            <img src={image} alt={product.name} />
          ) : (
            <span className="no-media-text">该商品暂无资料</span>
          )}
          {hasVideo && (
            <button
              type="button"
              className="media-toggle"
              onClick={() => setShowVideo((v) => !v)}
            >
              {showVideo ? '查看图片' : '播放视频'}
            </button>
          )}
        </div>

        <div>
          <h1 className="detail-title">{product.name}</h1>
          <p className="detail-subtitle">{product.subtitle}</p>
          <div className="detail-price">¥{price}</div>

          {skuList.length > 0 && (
            <div>
              <div className="sku-group-label">规格</div>
              <div className="sku-options">
                {skuList.map((sku) => (
                  <button
                    key={sku.id}
                    className={`sku-option ${selectedSku?.id === sku.id ? 'active' : ''}`}
                    onClick={() => setSelectedSku(sku)}
                  >
                    {sku.specDesc}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="sku-group-label">数量</div>
          <div className="qty-control">
            <button className="qty-btn" onClick={() => setQuantity((q) => Math.max(1, q - 1))}>−</button>
            <span>{quantity}</span>
            <button className="qty-btn" onClick={() => setQuantity((q) => q + 1)}>+</button>
          </div>

          <button className="btn btn-primary btn-block" onClick={handleAddToCart} disabled={adding}>
            {adding ? '添加中...' : '加入购物车'}
          </button>
          {message && <p className="form-footer">{message}</p>}

          {info2.length > 0 && (
            <div className="spec-table">
              {info2.map((spec) => (
                <div key={spec.id} className="spec-row">
                  <div className="spec-row-key">{spec.specKey}</div>
                  <div>{spec.specValue}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {info1.length > 0 && (
        <div className="detail-html">
          {info1.map((block) => (
            <div key={block.id}>
              {block.imageUrl && <img src={block.imageUrl} alt="" />}
              {block.detailHtml && <div dangerouslySetInnerHTML={{ __html: block.detailHtml }} />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
