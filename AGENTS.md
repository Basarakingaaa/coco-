# 项目上下文备忘

## 项目当前定位

这是一个面向 **Windows 本地部署和 IntelliJ IDEA 展示/开发** 的电商系统：

- 前端：React + Vite
- 后端：Spring Boot
- 数据库：MySQL
- 缓存：Redis
- 对象存储：MinIO
- 本地编排：Docker Compose

当前项目不再维护云端集群部署路径。解释性文档统一写在 `README.md`。

## 当前启动方式

推荐 IDEA 展示方式：

1. 打开 Docker Desktop。
2. 用 IDEA 打开项目根目录。
3. 在 IDEA Terminal 执行 `.\scripts\start-local-deps.ps1`，只启动 MySQL、Redis、MinIO。
4. 在 IDEA 里运行 `backend/src/main/java/com/mall/MallApplication.java`。
5. 在 IDEA 里创建 npm Run Configuration，执行 `frontend/package.json` 的 `dev` 脚本。
6. 浏览器访问 `http://localhost:5173`。

完整 Docker Compose 方式：

```powershell
.\scripts\start-local.ps1
```

停止：

```powershell
.\scripts\stop-local.ps1
```

清空本地 MySQL/MinIO 数据卷：

```powershell
.\scripts\stop-local.ps1 -WithData
```

## 关键约束

- 后端默认连接 `localhost:3307`、`localhost:6379`、`http://localhost:9000`，不需要额外 Spring profile。
- `sql/init.sql` 只会在 MySQL 数据卷首次创建时执行；改种子数据后要用 `.\scripts\stop-local.ps1 -WithData` 重建数据卷。
- `.env` 是本地私有配置，不提交。
- `RedisConfig` 里独立创建的 `ObjectMapper` 必须保留 `objectMapper.findAndRegisterModules()`，否则包含 `LocalDateTime` 的对象写 Redis 会失败。
- MySQL 初始化和手动导入都要使用 `utf8mb4`，否则中文数据可能写入时就变成乱码。

## 默认账号

- 管理员：`admin` / `admin123`
- MinIO：`minioadmin` / `minioadmin`
