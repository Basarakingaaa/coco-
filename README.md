# XX 电子商务系统

一套面向 Windows 本地运行和 IDEA 展示的电商系统：React 前端 + Spring Boot 后端 + MySQL + Redis + MinIO。项目默认按本地开发环境启动。

## 目录

- [功能架构](#功能架构)
- [本地架构](#本地架构)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [Windows 本地启动](#windows-本地启动)
- [IDEA 展示和启动](#idea-展示和启动)
- [常用操作](#常用操作)
- [常见问题](#常见问题)

## 功能架构

```text
XX 电子商务系统
├── 用户端
│   ├── 首页：轮播广告、分类导航、推荐商品
│   ├── 商品列表：分类筛选、关键字搜索
│   ├── 商品详情：SKU、规格参数、图文详情
│   ├── 购物车：勾选结算、数量调整
│   ├── 注册 / 登录：邮箱验证码、JWT 登录态
│   └── 结算下单：生成订单、扣减库存、模拟支付
└── 管理端
    ├── 商品管理：商品、SKU、图片/视频资料
    ├── 分类管理
    ├── 广告管理
    ├── 订单管理
    └── 用户管理
```

默认管理员账号：

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | 数据库初始化脚本内置账号，仅用于本地演示 |

## 本地架构

```text
浏览器
  │
  ├── http://localhost:5173
  │       React + Vite 开发服务器
  │       /api 代理到 http://localhost:8080
  │
  └── http://localhost:8080
          Spring Boot 后端
          ├── MySQL  localhost:3306  持久化数据
          ├── Redis  localhost:6379  缓存 / 验证码
          └── MinIO  localhost:9000  商品图片 / 视频
```

后端默认连接本地依赖：

- MySQL：`localhost:3306`
- Redis：`localhost:6379`
- MinIO：`http://localhost:9000`

当前推荐部署为 Windows 本机运行前后端、Windows 原生 MySQL、Docker Redis、Windows 原生 MinIO。Docker Compose 文件保留为可选备用路径，不作为当前答辩展示主路径。

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | React 18 + Vite + React Router + Axios |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + Spring Data JPA |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 本地部署 | Windows 原生依赖 + Redis Docker |
| 开发工具 | IntelliJ IDEA / PowerShell |

## 仓库结构

```text
coco-/
├── backend/                 # Spring Boot 后端
├── frontend/                # React 前端
├── sql/init.sql             # MySQL 初始化脚本
├── scripts/
│   ├── start-local-deps.ps1 # 可选备用：Docker 方式启动依赖
│   ├── start-local.ps1      # 可选备用：Docker Compose 启动
│   └── stop-local.ps1       # 可选备用：停止 Docker Compose
├── docker-compose.yml       # 可选备用，本次 Windows 原生展示不依赖
├── .env.example             # 本地环境变量模板
└── README.md
```

## Windows 本地启动

### 前置环境

- Windows 10/11
- PowerShell 5+ 或 PowerShell 7+
- JDK 17
- Maven 3.9+
- Node.js 20+
- IntelliJ IDEA
- Docker Desktop（仅用于 Redis 容器）
- MySQL 8/9 Windows 原生服务
- MinIO Windows 版 `minio.exe`

### 当前推荐部署方式

当前项目按 Windows 本地展示环境运行：

- MySQL：Windows 原生服务，端口 `3306`
- Redis：Docker 容器，端口 `6379`
- MinIO：Windows 原生 `minio.exe`，API 端口 `9000`，Console 端口 `9001`
- 后端：IDEA 直接运行 `MallApplication`
- 前端：`npm run dev`，端口 `5173`

访问地址：

| 服务 | 地址 |
|---|---|
| 前端 | `http://localhost:5173` |
| 后端健康检查 | `http://localhost:8080/actuator/health` |
| 商品接口 | `http://localhost:8080/api/products` |
| MinIO Console | `http://localhost:9001` |

### 启动 Redis

```powershell
docker start mall-local-redis-1
docker exec mall-local-redis-1 redis-cli ping
```

如果没有现成 Redis 容器：

```powershell
docker run -d --name mall-redis -p 6379:6379 redis:7-alpine
```

### 启动 MinIO

MinIO 服务窗口需要保持打开：

```powershell
$env:MINIO_ROOT_USER="minioadmin"
$env:MINIO_ROOT_PASSWORD="minioadmin"
& "C:\minio\minio.exe" server "C:\minio\data" --address ":9000" --console-address ":9001"
```

MinIO 默认账号：

| 用户名 | 密码 |
|---|---|
| `minioadmin` | `minioadmin` |

`mall-media` bucket 需要设置为公开只读：

```powershell
& "C:\minio\mc.exe" alias set local http://127.0.0.1:9000 minioadmin minioadmin
& "C:\minio\mc.exe" anonymous set download local/mall-media
```

### 初始化 MySQL

MySQL 使用 Windows 原生服务，后端默认连接 `localhost:3306`。首次安装后创建数据库和项目用户：

```sql
CREATE DATABASE mall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mall'@'localhost' IDENTIFIED BY '<本机数据库密码>';
CREATE USER 'mall'@'%' IDENTIFIED BY '<本机数据库密码>';
GRANT ALL PRIVILEGES ON mall.* TO 'mall'@'localhost';
GRANT ALL PRIVILEGES ON mall.* TO 'mall'@'%';
FLUSH PRIVILEGES;
```

导入初始数据：

```powershell
cmd /c """C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"" --default-character-set=utf8mb4 -uroot -p -P3306 mall < sql\init.sql"
```

### IDEA 启动后端

用 IntelliJ IDEA 打开项目根目录，不要只打开 `backend` 或 `frontend` 子目录。

后端主类：

```text
backend/src/main/java/com/mall/MallApplication.java
```

点击类左侧绿色运行按钮，确认 Run Configuration：

| 配置项 | 值 |
|---|---|
| JDK | `17` |
| Main class | `com.mall.MallApplication` |
| Working directory | 项目根目录或 `backend` 均可 |

后端不需要额外 Spring profile。IDEA Run Configuration 推荐配置环境变量：

```text
DB_HOST=localhost;DB_PORT=3306;DB_NAME=mall;DB_USERNAME=mall;DB_PASSWORD=<本机数据库密码>;REDIS_HOST=localhost;REDIS_PORT=6379;REDIS_PASSWORD=;MINIO_ENDPOINT=http://localhost:9000;MINIO_PUBLIC_URL=http://localhost:9000/mall-media;MINIO_ACCESS_KEY=minioadmin;MINIO_SECRET_KEY=minioadmin;MINIO_BUCKET=mall-media;MANAGEMENT_HEALTH_MAIL_ENABLED=false
```

启动成功后验证：

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/products
```

### IDEA 启动前端

第一次启动前端前，在 IDEA 底部 `Terminal` 执行：

```powershell
cd frontend
npm install
```

然后在 IDEA 创建 `npm` Run Configuration：

| 配置项 | 值 |
|---|---|
| package.json | `frontend/package.json` |
| Command | `run` |
| Scripts | `dev` |
| Working directory | `frontend` |

运行后访问：

```text
http://localhost:5173
```

前端的 `/api` 请求会由 `frontend/vite.config.js` 代理到 `http://localhost:8080`。

### 推荐演示顺序

1. 确认 Windows MySQL 服务已启动，端口为 `3306`
2. 打开 Docker Desktop，启动 Redis 容器
3. 启动 Windows MinIO，并确认 `mall-media` bucket 可公开读取
4. IDEA 打开项目根目录
5. IDEA 启动 `MallApplication`
6. IDEA 启动前端 npm `dev`
7. 浏览器打开 `http://localhost:5173`
8. 使用 `admin` / `admin123` 登录后台

## 常用操作

### 停止服务

- 后端和前端：点击 IDEA Run 窗口停止按钮
- MinIO：关闭运行 `minio.exe` 的 PowerShell 窗口，或按 `Ctrl+C`
- Redis：`docker stop mall-local-redis-1`
- MySQL：通常保持 Windows 服务运行即可，需要停止时在 Windows 服务管理器中停止 MySQL 服务

### 查看容器状态

当前只需要关注 Redis 容器：

```powershell
docker ps
```

### 查看日志

```powershell
docker logs -f mall-local-redis-1
```

### 修改端口、密码和邮箱配置

`.env.example` 提供本地变量示例，`.env` 是本机私有配置，不提交到 Git。

使用 IDEA 直接运行后端时，Spring Boot 不会自动读取项目根目录的 `.env`；需要在 IDEA 后端 Run Configuration 的 `Environment variables` 中配置，或配置为 Windows 系统环境变量。

默认端口：

| 服务 | 端口 |
|---|---:|
| 前端 | `5173` |
| 后端 | `8080` |
| MySQL | `3306` |
| Redis | `6379` |
| MinIO API | `9000` |
| MinIO Console | `9001` |

常用环境变量：

| 变量 | 默认值 | 作用 |
|---|---|---|
| `DB_NAME` | `mall` | 数据库名 |
| `DB_USERNAME` | `mall` | 后端连接 MySQL 的用户名 |
| `DB_PASSWORD` | `mall` | 后端连接 MySQL 的密码，实际本机密码建议只写在 IDEA 环境变量里 |
| `DB_HOST` | `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO API 地址 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO 用户名 |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO 密码 |
| `MINIO_BUCKET` | `mall-media` | MinIO bucket 名称 |
| `MINIO_PUBLIC_URL` | `http://localhost:9000/mall-media` | 文件公开访问前缀 |
| `JWT_SECRET` | `local-development-jwt-secret-change-before-production` | JWT 签名密钥 |
| `MAIL_HOST` | `smtp.example.com` | SMTP 服务器地址 |
| `MAIL_PORT` | `587` | SMTP 端口 |
| `MAIL_USERNAME` | 空 | 发件邮箱账号 |
| `MAIL_PASSWORD` | 空 | SMTP 授权码或应用专用密码 |

邮箱验证码需要配置这 4 个变量：

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

示例，QQ 邮箱：

```env
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=你的QQ邮箱
MAIL_PASSWORD=QQ邮箱SMTP授权码
```

示例，163 邮箱：

```env
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=你的163邮箱
MAIL_PASSWORD=163邮箱客户端授权码
```

`MAIL_PASSWORD` 通常不是邮箱登录密码，而是邮箱后台单独生成的 SMTP 授权码。修改邮箱配置后，需要重启后端服务。

## 常见问题

### 后端连不上 MySQL

先确认 Windows MySQL 服务已启动，并监听 `3306`：

```powershell
netstat -ano | findstr ":3306"
```

默认后端连接：

```text
jdbc:mysql://localhost:3306/mall
```

再确认 IDEA 后端环境变量中的 `DB_USERNAME` 和 `DB_PASSWORD` 与本机 MySQL 用户一致。

### 前端接口请求失败

确认后端接口能直接访问：

```text
http://localhost:8080/api/products
```

如果该地址不可访问，先解决后端启动问题；如果该地址可访问，再检查 `frontend/vite.config.js` 的代理配置。

### 页面图片或上传文件无法访问

确认 MinIO 正常运行：

```text
http://localhost:9001
```

本地默认公开访问地址是：

```text
http://localhost:9000/mall-media
```

### 注册验证码发不出去

本地默认没有配置真实 SMTP。演示时直接使用管理员账号 `admin` / `admin123` 登录即可。

如需真实邮箱验证码：

- IDEA 直接启动后端：在后端 Run Configuration 的 `Environment variables` 中配置 `MAIL_*`。
- `MAIL_PASSWORD` 应填写邮箱 SMTP 授权码，不是邮箱登录密码。

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

### 修改 `sql/init.sql` 后数据没变化

当前 MySQL 是 Windows 原生服务，修改 `sql/init.sql` 后需要手动重新导入，或先清空对应业务表后再导入。

导入时必须使用 `utf8mb4`：

```powershell
cmd /c """C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"" --default-character-set=utf8mb4 -uroot -p -P3306 mall < sql\init.sql"
```

如果前端商品、分类显示为 `æ— çº¿...` 这类乱码，说明数据曾经用错误客户端编码导入过。需要清理旧数据后，使用上面的 `--default-character-set=utf8mb4` 命令重新导入。

## Windows 原生依赖部署总结

本项目当前可采用混合本地部署方式，适合在 Windows + IntelliJ IDEA 环境中展示和调试：

```text
前端：Windows 本机 npm run dev，端口 5173
后端：Windows 本机 IDEA 运行 MallApplication，端口 8080
MySQL：Windows 原生服务，端口 3306
Redis：Docker 容器，映射到 localhost:6379
MinIO：Windows 原生 minio.exe，API 端口 9000，Console 端口 9001
```

### MySQL 原生部署

MySQL 安装在 Windows 本机，后端默认连接：

```text
jdbc:mysql://localhost:3306/mall
```

初始化步骤：

```sql
CREATE DATABASE mall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mall'@'localhost' IDENTIFIED BY '<本机数据库密码>';
CREATE USER 'mall'@'%' IDENTIFIED BY '<本机数据库密码>';
GRANT ALL PRIVILEGES ON mall.* TO 'mall'@'localhost';
GRANT ALL PRIVILEGES ON mall.* TO 'mall'@'%';
FLUSH PRIVILEGES;
```

导入初始数据时必须使用 `utf8mb4`：

```powershell
cmd /c """C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe"" --default-character-set=utf8mb4 -uroot -p -P3306 mall < sql\init.sql"
```

验证：

```sql
SHOW TABLES;
SELECT username FROM t_user;
```

### Redis Docker 部署

Redis 使用 Docker 容器即可，后端默认连接 `localhost:6379`。

```powershell
docker start mall-local-redis-1
docker exec mall-local-redis-1 redis-cli ping
```

如果没有现成容器，可新建：

```powershell
docker run -d --name mall-redis -p 6379:6379 redis:7-alpine
```

### MinIO Windows 原生部署

MinIO 使用 Windows 版 `minio.exe`：

```powershell
$env:MINIO_ROOT_USER="minioadmin"
$env:MINIO_ROOT_PASSWORD="minioadmin"
& "C:\minio\minio.exe" server "C:\minio\data" --address ":9000" --console-address ":9001"
```

浏览器访问：

```text
http://localhost:9001
```

创建 bucket：

```text
mall-media
```

使用 `mc.exe` 设置 bucket 公开只读：

```powershell
& "C:\minio\mc.exe" alias set local http://127.0.0.1:9000 minioadmin minioadmin
& "C:\minio\mc.exe" anonymous set download local/mall-media
& "C:\minio\mc.exe" anonymous get local/mall-media
```

### IDEA 后端环境变量

后端直接在 IDEA 中运行时，推荐配置：

```text
DB_HOST=localhost;DB_PORT=3306;DB_NAME=mall;DB_USERNAME=mall;DB_PASSWORD=<本机数据库密码>;REDIS_HOST=localhost;REDIS_PORT=6379;REDIS_PASSWORD=;MINIO_ENDPOINT=http://localhost:9000;MINIO_PUBLIC_URL=http://localhost:9000/mall-media;MINIO_ACCESS_KEY=minioadmin;MINIO_SECRET_KEY=minioadmin;MINIO_BUCKET=mall-media;MANAGEMENT_HEALTH_MAIL_ENABLED=false
```

如需真实发送注册验证码，再追加真实 SMTP 配置：

```text
MAIL_HOST=smtp.qq.com;MAIL_PORT=587;MAIL_USERNAME=<邮箱账号>;MAIL_PASSWORD=<SMTP授权码>
```

`MAIL_PASSWORD` 不应使用邮箱登录密码，应使用邮箱后台生成的 SMTP 授权码。

## 答辩草稿

各位老师好，我的项目是一个面向本地部署和演示的电商系统，整体采用前后端分离架构。前端使用 React + Vite，后端使用 Spring Boot，数据持久化使用 MySQL，缓存和验证码临时数据使用 Redis，商品图片和视频等媒体资源使用 MinIO 对象存储。

系统功能分为用户端和管理端。用户端包括首页商品展示、分类浏览、商品详情、购物车、注册登录和下单流程；管理端包括商品管理、分类管理、广告管理、订单管理和用户管理。系统内置管理员账号，便于本地演示和功能验证。

本项目的部署重点是 Windows 本地可复现。前端和后端直接在 Windows 与 IntelliJ IDEA 中运行，MySQL 采用 Windows 原生服务，Redis 使用 Docker 容器，MinIO 使用 Windows 原生可执行文件。这样的组合减少了完整容器化对展示环境的依赖，同时保留 Redis 容器的轻量启动优势。

数据库初始化通过 `sql/init.sql` 完成，导入时明确使用 `utf8mb4` 编码，避免中文商品、分类和广告数据出现乱码。后端通过环境变量配置数据库、Redis、MinIO 和邮件服务，避免把本机私密配置写入代码仓库。

后端核心采用 Spring Boot 分层结构，Controller 负责接口入口，Service 处理业务逻辑，Repository 负责数据访问。用户登录使用 JWT，注册验证码通过邮件发送并结合 Redis 设置过期时间和发送冷却时间。商品媒体文件上传到 MinIO，数据库只保存访问地址和业务关联信息。

在本地演示时，启动顺序是先确认 MySQL、Redis、MinIO 可用，再运行后端 `MallApplication`，最后进入 `frontend` 执行 `npm run dev`，浏览器访问 `http://localhost:5173`。默认管理员账号为 `admin / admin123`。

本项目的设计取舍是优先保证本地展示稳定、启动路径清晰、配置可解释。后续如果继续完善，可以补充支付接口、订单状态流转、库存并发控制、邮件配置页面，以及更完整的自动化测试和生产环境部署方案。
