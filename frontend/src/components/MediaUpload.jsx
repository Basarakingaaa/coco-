import { useRef, useState } from 'react';
import { adminApi } from '../api';

// 旧文件的最佳努力清理：删除接口只接受我们自己 MinIO 存储里的 URL（种子数据里的占位图等
// 外部地址会被后端拒绝），所以失败时静默忽略，不阻塞用户的上传/移除操作。
async function tryDeleteOldFile(url) {
  if (!url) return;
  try {
    await adminApi.deleteFile(url);
  } catch {
    // ignore
  }
}

export default function MediaUpload({ label, value, onChange, accept = 'image/*', isVideo = false }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [error, setError] = useState('');

  const handleFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const res = await adminApi.upload(file);
      await tryDeleteOldFile(value);
      onChange(res.data.url);
    } catch (err) {
      setError(err.message || '上传失败');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const handleRemove = async () => {
    setRemoving(true);
    setError('');
    try {
      await tryDeleteOldFile(value);
      onChange('');
    } finally {
      setRemoving(false);
    }
  };

  return (
    <div className="form-field">
      <label>{label}</label>
      <div className="upload-box">
        <div className="upload-preview">
          {value ? (
            isVideo ? <video src={value} muted /> : <img src={value} alt="" />
          ) : (
            <span style={{ fontSize: 11, color: '#86868b' }}>无</span>
          )}
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => inputRef.current?.click()}
          disabled={uploading || removing}
        >
          {uploading ? '上传中...' : value ? '更换文件' : '上传文件'}
        </button>
        {value && (
          <button
            type="button"
            className="admin-link-btn danger"
            onClick={handleRemove}
            disabled={uploading || removing}
          >
            {removing ? '删除中...' : '移除'}
          </button>
        )}
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          style={{ display: 'none' }}
          onChange={handleFile}
        />
      </div>
      {error && <p className="form-error">{error}</p>}
    </div>
  );
}
