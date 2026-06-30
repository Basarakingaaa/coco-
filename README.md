# XX 电子商务系统

这是一个面向 **Windows 本地部署、IntelliJ IDEA 展示与开发** 的电商系统。项目包含 React 前端、Spring Boot 后端、MySQL、Redis、MinIO，对外提供用户端购物流程和后台管理功能。

当前版本已经调整为 Windows 本地运行方式：不依赖 K8s、APISIX 或远程服务器。推荐在答辩/演示时使用 Docker Desktop 启动基础依赖，再用 IDEA 分别启动后端和前端。

## 目录

- [功能概览](#功能概览)
- [本地架构](#本地架构)
- [系统设计说明](#系统设计说明)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [部署前准备](#部署前准备)
- [部署方式一：IDEA 展示模式](#部署方式一idea-展示模式)
- [部署方式二：完整 Docker Compose 模式](#部署方式二完整-docker-compose-模式)
- [环境变量说明](#环境变量说明)
- [启动后验证](#启动后验证)
- [常用维护命令](#常用维护命令)
- [常见问题](#常见问题)

## 功能概览

```text
XX 电子商务系统
├── 用户端
│   ├── 首页：轮播广告、分类导航、推荐商品
│   ├── 商品列表：分类筛选、关键字搜索
│   ├── 商品详情：商品资料、规格参数、SKU
│   ├── 购物车：添加商品、修改数量、删除商品
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

当前项目采用 **Windows 本地单机部署 + 前后端分离** 架构。基础依赖由 Docker Compose 管理，业务代码由 IDEA 启动和展示。

```text
Windows 本机
  │
  ├── 浏览器
  │     └── http://localhost:5173
  │
  ├── 前端层：React + Vite
  │     ├── 页面路由：React Router
  │     ├── 接口请求：Axios
  │     └── 开发代理：/api -> http://localhost:8080
  │
  ├── 后端层：Spring Boot
  │     ├── Controller：HTTP API
  │     ├── Service：业务逻辑
  │     ├── Repository：数据访问
  │     ├── Security：JWT 鉴权和角色控制
  │     └── Config：Redis / MinIO / Security 配置
  │
  └── 基础依赖层：Docker Compose
        ├── MySQL  localhost:3307  业务数据
        ├── Redis  localhost:6379  缓存 / 验证码
        └── MinIO  localhost:9000  图片 / 视频对象存储
```

说明：

- 前端通过 Vite 启动，访问地址为 `http://localhost:5173`。
- 后端通过 IDEA 直接运行 `MallApplication`，访问地址为 `http://localhost:8080`。
- MySQL 使用 `3307` 是为了避开 Windows 本机已有 MySQL 常用的 `3306`。
- Redis 用于商品详情缓存、验证码缓存等场景。
- MinIO 用于后台上传商品图片、视频等资料。
- 当前本地版本没有网关层，请求由 Vite 开发代理直接转发到后端。
- 当前本地版本没有集群调度层，MySQL、Redis、MinIO 都是 Docker Compose 单机容器。

## 系统设计说明

### 请求链路

用户访问前端页面后，前端通过 Axios 调用 `/api` 开头的接口。开发环境中，Vite 根据 `frontend/vite.config.js` 将 `/api` 请求代理到后端：

```text
浏览器 -> React/Vite -> /api 代理 -> Spring Boot -> MySQL / Redis / MinIO
```

这种设计的好处是：

- 前端代码中只需要写相对路径 `/api`，不用硬编码后端地址。
- 本地开发时避免浏览器跨域问题。
- 以后如果切换到 Nginx 或其他网关，只需要调整代理层，不需要大改业务代码。

### 后端分层

后端采用常见的 Spring Boot 分层结构：

| 层级 | 目录 | 职责 |
|---|---|---|
| Controller | `backend/src/main/java/com/mall/controller` | 接收 HTTP 请求，做参数校验和结果返回 |
| Service | `backend/src/main/java/com/mall/service` | 编排业务逻辑，例如下单、购物车、验证码 |
| Repository | `backend/src/main/java/com/mall/repository` | 通过 Spring Data JPA 访问 MySQL |
| Model / DTO | `backend/src/main/java/com/mall/model` | 定义数据库实体和接口数据对象 |
| Security | `backend/src/main/java/com/mall/security` | JWT 生成、解析和登录态校验 |
| Config | `backend/src/main/java/com/mall/config` | Redis、MinIO、Spring Security 等配置 |

### 数据存储设计

系统把不同类型的数据放到不同组件中：

| 数据类型 | 存储位置 | 原因 |
|---|---|---|
| 用户、商品、订单、购物车 | MySQL | 结构化业务数据，需要事务和持久化 |
| 验证码、商品详情缓存 | Redis | 需要过期时间或快速读写 |
| 商品图片、视频 | MinIO | 文件数据不适合直接存数据库，适合对象存储 |

### 登录与权限设计

登录成功后，后端签发 JWT。前端保存 token，并在后续请求中携带：

```text
Authorization: Bearer <token>
```

后端通过 `JwtAuthFilter` 解析 token，确认当前用户身份。后台接口要求用户角色为 `ADMIN`，普通用户不能访问后台管理接口。

### 文件上传设计

后台商品管理中上传图片或视频时，请求先到 Spring Boot 后端。后端校验文件类型和大小后，把文件上传到 MinIO，并把最终访问 URL 保存到数据库。

```text
浏览器上传文件 -> Spring Boot 校验 -> MinIO 存储文件 -> MySQL 保存 URL
```

这种设计避免把大文件直接写入 MySQL，数据库只保存结构化信息和文件地址。

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | React 18、Vite、React Router、Axios |
| 后端 | Spring Boot 3.2、Spring Security、JWT、Spring Data JPA |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7 |
| 对象存储 | MinIO |
| 本地编排 | Docker Compose |
| 开发工具 | IntelliJ IDEA、PowerShell |

## 目录结构

```text
coco-/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java         # 后端业务代码
│   ├── src/main/resources    # application.yml 等配置
│   ├── pom.xml               # Maven 依赖
│   └── settings.xml          # Maven 阿里云镜像配置
├── frontend/                # React 前端
│   ├── src                   # 前端页面、组件、API 封装
│   ├── package.json          # 前端依赖和脚本
│   └── vite.config.js        # Vite 代理配置
├── sql/
│   ├── init.sql              # MySQL 初始化 SQL
│   └── init-local.sh         # Docker 初始化导入脚本
├── scripts/
│   ├── start-local-deps.ps1  # 只启动 MySQL / Redis / MinIO
│   ├── start-local.ps1       # 启动完整 Docker Compose 环境
│   └── stop-local.ps1        # 停止本地环境
├── docker-compose.yml        # Windows 本地 Compose 编排
├── .env.example              # 本地环境变量模板
├── README.md                 # 部署和使用说明
└── 答辩草稿.md               # 答辩讲稿与可能问题
```

## 部署前准备

### 1. 安装软件

Windows 本地部署需要：

- Docker Desktop，启用 Linux containers
- IntelliJ IDEA
- JDK 17
- Maven 3.9+
- Node.js 20+
- PowerShell 5+ 或 PowerShell 7+

### 2. 确认 Docker 可用

打开 Docker Desktop。如果出现 `Virtualization support not detected`，需要先开启虚拟化：

1. 在 BIOS 中启用 Intel VT-x 或 AMD SVM。
2. 在 Windows 中启用 WSL2 和虚拟机平台。
3. 管理员 PowerShell 执行：

```powershell
wsl --set-default-version 2
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All
Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All
bcdedit /set hypervisorlaunchtype auto
```

执行后重启电脑，再重新打开 Docker Desktop。

### 3. 打开项目

用 IDEA 打开项目根目录，例如：

```text
C:\Users\Administrator\Desktop\新建文件夹 (3)
```

不要只打开 `backend` 或 `frontend` 子目录，否则 Maven、前端和脚本结构不完整。

## 部署方式一：IDEA 展示模式

这是推荐部署方式，适合本地开发、课堂展示和答辩演示。

### 第一步：启动基础依赖

在项目根目录执行：

```powershell
.\scripts\start-local-deps.ps1
```

该脚本会：

1. 如果 `.env` 不存在，自动从 `.env.example` 复制一份。
2. 使用 Docker Compose 启动 MySQL、Redis、MinIO。
3. 只启动基础依赖，不启动后端和前端源码。

启动成功后，依赖地址如下：

| 服务 | 地址 |
|---|---|
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |
| MinIO API | `http://localhost:9000` |
| MinIO Console | `http://localhost:9001` |

MinIO 默认账号：

| 用户名 | 密码 |
|---|---|
| `minioadmin` | `minioadmin` |

### 第二步：在 IDEA 启动后端

打开后端主类：

```text
backend/src/main/java/com/mall/MallApplication.java
```

点击绿色运行按钮，运行 `MallApplication`。

后端运行配置建议确认：

| 配置项 | 值 |
|---|---|
| JDK | `17` |
| Main class | `com.mall.MallApplication` |
| Working directory | 项目根目录或 `backend` 均可 |

当前后端默认连接本地依赖，不需要额外设置 Spring Profile。

后端启动成功后访问：

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/products
```

### 第三步：在 IDEA 启动前端

第一次启动前端前，在 IDEA Terminal 执行：

```powershell
cd frontend
npm install
```

然后启动前端：

```powershell
npm run dev
```

或在 IDEA 创建 npm Run Configuration：

| 配置项 | 值 |
|---|---|
| package.json | `frontend/package.json` |
| Command | `run` |
| Scripts | `dev` |
| Working directory | `frontend` |

前端启动成功后访问：

```text
http://localhost:5173
```

### 第四步：登录系统

后台入口使用普通登录页：

```text
http://localhost:5173/login
```

默认管理员账号：

```text
admin / admin123
```

管理员登录后会进入 `/admin` 后台页面。

## 部署方式二：完整 Docker Compose 模式

该方式适合快速运行完整系统，不需要 IDEA 分别启动前后端。

在项目根目录执行：

```powershell
.\scripts\start-local.ps1
```

它会启动：

- MySQL
- Redis
- MinIO
- 后端容器
- 前端容器

访问地址：

| 服务 | 地址 |
|---|---|
| 前端 | `http://localhost:5173` |
| 后端健康检查 | `http://localhost:8080/actuator/health` |
| 商品接口 | `http://localhost:8080/api/products` |
| MinIO Console | `http://localhost:9001` |

说明：

- 完整 Docker Compose 模式更接近“一键部署”。
- IDEA 展示模式更适合答辩，因为可以直接展示源码、断点和日志。

## 环境变量说明

首次启动脚本会自动生成 `.env`。`.env` 是本机私有配置，不提交到 Git。

### 基础配置

| 变量 | 默认值 | 作用 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | `root123456` | MySQL root 密码 |
| `DB_NAME` | `mall` | 数据库名 |
| `DB_USERNAME` | `mall` | 后端连接 MySQL 的用户名 |
| `DB_PASSWORD` | `mall` | 后端连接 MySQL 的密码 |
| `MYSQL_PORT` | `3307` | MySQL 暴露到 Windows 的端口 |
| `REDIS_PORT` | `6379` | Redis 暴露到 Windows 的端口 |
| `MINIO_API_PORT` | `9000` | MinIO API 端口 |
| `MINIO_CONSOLE_PORT` | `9001` | MinIO 控制台端口 |
| `BACKEND_PORT` | `8080` | 后端端口 |
| `FRONTEND_PORT` | `5173` | 前端端口 |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO 用户名 |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO 密码 |
| `MINIO_BUCKET` | `mall-media` | MinIO bucket 名称 |
| `JWT_SECRET` | `local-development-jwt-secret-change-before-production` | JWT 签名密钥 |

### 邮箱验证码配置

邮箱验证码功能仍然保留。要真实发送邮件，需要配置：

| 变量 | 说明 |
|---|---|
| `MAIL_HOST` | SMTP 服务器地址 |
| `MAIL_PORT` | SMTP 端口 |
| `MAIL_USERNAME` | 发件邮箱账号 |
| `MAIL_PASSWORD` | SMTP 授权码或应用专用密码 |

QQ 邮箱示例：

```env
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=你的QQ邮箱
MAIL_PASSWORD=QQ邮箱SMTP授权码
```

163 邮箱示例：

```env
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=你的163邮箱
MAIL_PASSWORD=163邮箱客户端授权码
```

注意：

- `MAIL_PASSWORD` 通常不是邮箱登录密码，而是邮箱后台生成的 SMTP 授权码。
- 如果使用 IDEA 直接运行后端，Spring Boot 不会自动读取项目根目录的 `.env`。
- IDEA 方式需要在 Run Configuration 的 `Environment variables` 中配置 `MAIL_*`，或配置为 Windows 用户环境变量。

PowerShell 设置 Windows 用户环境变量示例：

```powershell
[Environment]::SetEnvironmentVariable("MAIL_HOST", "smtp.qq.com", "User")
[Environment]::SetEnvironmentVariable("MAIL_PORT", "587", "User")
[Environment]::SetEnvironmentVariable("MAIL_USERNAME", "你的QQ邮箱", "User")
[Environment]::SetEnvironmentVariable("MAIL_PASSWORD", "你的SMTP授权码", "User")
```

设置完成后要完全关闭并重新打开 IDEA。

## 启动后验证

### 1. 检查容器状态

```powershell
docker compose ps
```

正常情况下 MySQL、Redis、MinIO 应为 `healthy`。

### 2. 检查数据库数据

```powershell
docker compose exec -T mysql mysql --default-character-set=utf8mb4 -umall -pmall mall -e "SELECT id,name FROM t_product LIMIT 5;"
```

如果返回中文商品名，说明数据库编码正常。

### 3. 检查后端接口

```text
http://localhost:8080/api/products
```

正常返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": []
  }
}
```

实际 `content` 会包含初始化商品数据。

### 4. 检查前端页面

访问：

```text
http://localhost:5173
```

页面应能显示商品列表、分类、价格、图片。

### 5. 检查后台登录

访问：

```text
http://localhost:5173/login
```

使用：

```text
admin / admin123
```

登录后应进入后台管理页面。

## 常用维护命令

### 停止服务

保留 MySQL / MinIO 数据：

```powershell
.\scripts\stop-local.ps1
```

删除 MySQL / MinIO 数据卷：

```powershell
.\scripts\stop-local.ps1 -WithData
```

### 重新初始化数据库

如果修改了 `sql/init.sql`，需要删除数据卷后重新启动：

```powershell
.\scripts\stop-local.ps1 -WithData
.\scripts\start-local-deps.ps1
```

### 查看日志

```powershell
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f minio
docker compose logs -f backend
docker compose logs -f frontend
```

### 前端重新安装依赖

```powershell
cd frontend
npm install
```

如果 `npm install` 提示漏洞，演示阶段不建议直接执行 `npm audit fix --force`，因为它可能升级大版本依赖并引入兼容问题。

### 后端 Maven 依赖下载失败

IDEA 中设置 Maven 的 `User settings file` 为：

```text
backend/settings.xml
```

该文件配置了阿里云 Maven 镜像。也可以在命令行执行：

```powershell
cd backend
mvn -s settings.xml dependency:resolve
```

## 常见问题

### 1. Docker Desktop 提示没有虚拟化支持

先检查任务管理器 CPU 页面中的“虚拟化”是否启用。如果未启用，需要进 BIOS 打开 Intel VT-x 或 AMD SVM。

### 2. MySQL 3306 端口被占用

本项目默认使用 `3307` 暴露 MySQL，这是为了避开本机已有 MySQL 的 `3306`。

后端默认连接：

```text
jdbc:mysql://localhost:3307/mall
```

### 3. 前端显示中文乱码

原因通常是旧 MySQL 数据卷曾经用错误客户端编码导入过中文。解决方式：

```powershell
.\scripts\stop-local.ps1 -WithData
.\scripts\start-local-deps.ps1
```

项目的初始化脚本会使用：

```text
mysql --default-character-set=utf8mb4
```

保证中文正确写入数据库。

### 4. 后端报 `Unsupported character encoding 'utf8mb4'`

JDBC URL 的 `characterEncoding` 应为 `UTF-8`，不是 `utf8mb4`。当前配置已修正为：

```text
characterEncoding=UTF-8
```

### 5. IDEA 里 Lombok 或 Spring 依赖变红

通常是 Maven 没正确导入。处理方式：

1. 右键 `backend/pom.xml`，选择添加为 Maven 项目。
2. Maven 面板点击 Reload。
3. Maven 设置里使用 `backend/settings.xml`。
4. 启用 Lombok 插件和 Annotation Processing。

### 6. 邮箱验证码发不出去

先确认 SMTP 服务已开启，并使用的是授权码，不是邮箱登录密码。QQ 邮箱如果返回 `535 Login fail`，通常是：

- SMTP 服务未开启
- 授权码错误或过期
- 账号异常
- 登录频率限制

### 7. `/actuator/health` 显示 mail 不健康

如果只是本地演示，可以暂时忽略。也可以关闭邮件健康检查：

```text
MANAGEMENT_HEALTH_MAIL_ENABLED=false
```

但关闭健康检查不等于邮件可以发送，真正发送验证码仍需要正确 SMTP 配置。
