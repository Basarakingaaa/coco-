import { useEffect, useState } from 'react';
import { adminApi } from '../../api';

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const res = await adminApi.listUsers({ page: 1, size: 50 });
    setUsers(res.data?.content || []);
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const toggleStatus = async (user) => {
    await adminApi.updateUserStatus(user.id, user.status === 1 ? 0 : 1);
    await load();
  };

  return (
    <div>
      <div className="admin-header">
        <h1 className="admin-title">用户管理</h1>
      </div>

      {loading ? (
        <p>加载中...</p>
      ) : (
        <table className="admin-table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>邮箱</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.phone}</td>
                <td>{u.role}</td>
                <td>
                  <span className={`admin-badge ${u.status === 1 ? 'on' : 'off'}`}>
                    {u.status === 1 ? '启用' : '禁用'}
                  </span>
                </td>
                <td>
                  <button className="admin-link-btn" onClick={() => toggleStatus(u)}>
                    {u.status === 1 ? '禁用' : '启用'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
