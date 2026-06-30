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

如果后端运行在 Docker Compose 容器内，`docker-compose.yml` 会通过环境变量把连接地址改成 Compose 内部服务名。

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | React 18 + Vite + React Router + Axios |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + Spring Data JPA |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 本地编排 | Docker Compose |
| 开发工具 | IntelliJ IDEA / PowerShell |

## 仓库结构

```text
coco-/
├── backend/                 # Spring Boot 后端
├── frontend/                # React 前端
├── sql/init.sql             # MySQL 初始化脚本
├── scripts/
│   ├── start-local-deps.ps1 # 只启动 MySQL/Redis/MinIO，适合 IDEA 展示
│   ├── start-local.ps1      # 一键启动完整 Docker Compose 环境
│   └── stop-local.ps1       # 停止本地环境
├── docker-compose.yml       # Windows 本地 Compose 编排
├── .env.example             # 本地环境变量模板
└── README.md
```

## Windows 本地启动

### 前置环境

- Windows 10/11
- Docker Desktop，并启用 Linux containers
- PowerShell 5+ 或 PowerShell 7+

如果要在 IDEA 中直接运行源码，还需要：

- JDK 17
- Maven 3.9+
- Node.js 20+
- IntelliJ IDEA

### 方式一：完整 Docker Compose 启动

适合快速运行完整系统，不需要在 IDEA 中分别启动前后端。

在项目根目录执行：

```powershell
.\scripts\start-local.ps1
```

脚本会启动：

- MySQL
- Redis
- MinIO
- Spring Boot 后端容器
- React + Nginx 前端容器

访问地址：

| 服务 | 地址 |
|---|---|
| 前端 | `http://localhost:5173` |
| 后端健康检查 | `http://localhost:8080/actuator/health` |
| 商品接口 | `http://localhost:8080/api/products` |
| MinIO Console | `http://localhost:9001` |

MinIO 默认账号：

| 用户名 | 密码 |
|---|---|
| `minioadmin` | `minioadmin` |

### 方式二：只启动依赖，IDEA 启动源码

这是课堂展示、答辩演示和本地开发推荐方式。

在 IDEA 底部 `Terminal` 或项目根目录 PowerShell 执行：

```powershell
.\scripts\start-local-deps.ps1
```

该脚本只启动：

- MySQL：`localhost:3306`
- Redis：`localhost:6379`
- MinIO：`http://localhost:9000`
- MinIO Console：`http://localhost:9001`

后端和前端按下面的 IDEA 方式启动。

## IDEA 展示和启动

### 打开项目

用 IntelliJ IDEA 打开项目根目录：

```text
C:\Users\Administrator\Desktop\新建文件夹 (3)
```

不要只打开 `backend` 或 `frontend` 子目录，否则项目结构展示不完整。

### 启动依赖

打开 Docker Desktop 后，在 IDEA 底部 `Terminal` 执行：

```powershell
.\scripts\start-local-deps.ps1
```

### 启动后端

打开后端主类：

```text
backend/src/main/java/com/mall/MallApplication.java
```

点击类左侧绿色运行按钮，确认 Run Configuration：

| 配置项 | 值 |
|---|---|
| JDK | `17` |
| Main class | `com.mall.MallApplication` |
| Working directory | 项目根目录或 `backend` 均可 |

现在后端默认就是 Windows 本地配置，不需要额外设置 Spring profile。

启动成功后验证：

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/products
```

### 启动前端

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

1. 打开 Docker Desktop
2. IDEA 打开项目根目录
3. IDEA Terminal 执行 `.\scripts\start-local-deps.ps1`
4. IDEA 启动 `MallApplication`
5. IDEA 启动前端 npm `dev`
6. 浏览器打开 `http://localhost:5173`
7. 使用 `admin` / `admin123` 登录后台

## 常用操作

### 停止服务

停止后端和前端：

- IDEA 启动的服务：点击 IDEA Run 窗口停止按钮
- Docker Compose 启动的完整环境：执行停止脚本

保留 MySQL/MinIO 数据：

```powershell
.\scripts\stop-local.ps1
```

删除 MySQL/MinIO 数据卷，下一次启动会重新导入 `sql/init.sql`：

```powershell
.\scripts\stop-local.ps1 -WithData
```

### 查看容器状态

```powershell
docker compose ps
```

### 查看日志

```powershell
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f minio
docker compose logs -f backend
docker compose logs -f frontend
```

### 修改端口、密码和邮箱配置

首次运行脚本时会自动从 `.env.example` 复制 `.env`。本地端口、数据库密码、JWT 密钥、MinIO 密钥都在 `.env` 中配置。

`.env` 是本机私有配置，不提交到 Git。

需要注意：

- 使用 `.\scripts\start-local.ps1` 或 `docker compose up` 启动完整环境时，Docker Compose 会自动读取 `.env`。
- 使用 IDEA 直接运行后端时，Spring Boot 不会自动读取项目根目录的 `.env`；需要在 IDEA 后端 Run Configuration 的 `Environment variables` 里手动配置，或配置为 Windows 系统环境变量。

默认端口：

| 服务 | 端口 |
|---|---:|
| 前端 | `5173` |
| 后端 | `8080` |
| MySQL | `3306` |
| Redis | `6379` |
| MinIO API | `9000` |
| MinIO Console | `9001` |

端口被占用时，修改 `.env` 后重新启动：

```powershell
docker compose up -d
```

常用环境变量：

| 变量 | 默认值 | 作用 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | `root123456` | MySQL root 密码，仅 Docker Compose 初始化 MySQL 时使用 |
| `DB_NAME` | `mall` | 数据库名 |
| `DB_USERNAME` | `mall` | 后端连接 MySQL 的用户名 |
| `DB_PASSWORD` | `mall` | 后端连接 MySQL 的密码 |
| `MYSQL_PORT` | `3306` | MySQL 暴露到 Windows 的端口 |
| `REDIS_PORT` | `6379` | Redis 暴露到 Windows 的端口 |
| `MINIO_API_PORT` | `9000` | MinIO API 端口 |
| `MINIO_CONSOLE_PORT` | `9001` | MinIO 控制台端口 |
| `BACKEND_PORT` | `8080` | 后端端口 |
| `FRONTEND_PORT` | `5173` | 前端端口 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO 用户名 |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO 密码 |
| `MINIO_BUCKET` | `mall-media` | MinIO bucket 名称 |
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

先确认依赖容器已启动：

```powershell
docker compose ps
```

再确认 MySQL 端口没有被本机其他服务占用。默认后端连接：

```text
jdbc:mysql://localhost:3306/mall
```

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

- Docker Compose 完整启动：修改 `.env` 里的 `MAIL_*` 配置后重启后端容器。
- IDEA 直接启动后端：在后端 Run Configuration 的 `Environment variables` 中配置 `MAIL_*`。

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

### 修改 `sql/init.sql` 后数据没变化

MySQL 初始化脚本只在数据卷首次创建时执行。需要清空数据卷后重新启动：

```powershell
.\scripts\stop-local.ps1 -WithData
.\scripts\start-local-deps.ps1
```

如果前端商品、分类显示为 `æ— çº¿...` 这类乱码，说明旧数据卷曾经用错误客户端编码导入过数据。同样执行上面的 `-WithData` 清空数据卷后重启即可；本项目的 Docker 初始化脚本会使用 `mysql --default-character-set=utf8mb4` 导入 `sql/init.sql`。

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
