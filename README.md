# XX 电子商务系统

这是一个面向 Windows 本地部署和 IntelliJ IDEA 展示的电商系统。当前版本已全量重写为：

```text
服务端：Maven WebApp + Servlet + JDBC
前端：纯 HTML + CSS + jQuery
数据库：MySQL，沿用原 `sql/init.sql` 数据库结构
缓存：Redis，用于验证码缓存、发送冷却和商品查询缓存
对象存储：MinIO，可用于商品媒体文件
运行容器：Tomcat 10+
```

## 功能模块

用户端：

- 首页广告、分类和推荐商品展示
- 商品列表、分类筛选、关键词搜索
- 商品详情、商品资料、规格参数展示
- 登录、注册、退出登录
- 购物车添加、修改数量、删除
- 结算下单、订单列表、订单详情、模拟支付、取消订单

管理端：

- 商品管理：新增、编辑、下架、资料维护
- 分类管理：新增、编辑、禁用
- 广告管理：新增、编辑、禁用
- 订单管理：查看订单、修改订单状态
- 用户管理：查看用户、启用/禁用用户

默认管理员：

| 用户名 | 密码 |
|---|---|
| `admin` | `admin123` |

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | 纯 HTML + CSS + jQuery |
| 后端 | Maven WebApp + Jakarta Servlet |
| 数据访问 | JDBC + MySQL Connector/J |
| 登录态 | HttpSession |
| 密码校验 | BCrypt |
| 数据库 | MySQL 8/9 |
| 缓存 | Redis 7 |
| 运行容器 | Tomcat 10+ |

## 仓库结构

```text
coco-/
├── backend/
│   ├── pom.xml                         # Maven WebApp，打包为 WAR
│   └── src/main/
│       ├── java/com/mall/web/
│       │   ├── filter/EncodingFilter.java
│       │   ├── servlet/MallServlet.java
│       │   └── util/
│       │       ├── Db.java
│       │       └── Web.java
│       ├── resources/db.properties      # 默认数据库连接，可被环境变量覆盖
│       └── webapp/
│           ├── index.html               # jQuery 单页界面
│           ├── assets/css/app.css
│           ├── assets/js/app.js
│           └── WEB-INF/web.xml
├── sql/init.sql                         # 数据库结构和初始数据，不改变
├── scripts/                             # 本地辅助脚本
└── README.md
```

## 数据库

数据库结构不改，继续使用 `sql/init.sql`。

本机 MySQL 推荐配置：

```text
Host: localhost
Port: 3306
Database: mall
Username: mall
Password: 使用本机实际密码
Charset: utf8mb4
```

## Redis

当前版本需要 Redis，后端用途：

- 注册验证码缓存：`verify:register:{email}`，5 分钟过期
- 验证码发送冷却：`verify:cooldown:{email}`，60 秒过期
- 商品列表缓存：`products:list:*`，60 秒过期
- 商品详情缓存：`products:detail:{id}`，60 秒过期

本机推荐：

```text
Host: localhost
Port: 6379
Password: 空
```

Docker 启动：

```powershell
docker start mall-local-redis-1
```

如果没有现成容器：

```powershell
docker run -d --name mall-redis -p 6379:6379 redis:7-alpine
```

验证：

```powershell
docker exec -it mall-local-redis-1 redis-cli ping
```

返回 `PONG` 即可。

首次初始化：

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

验证：

```sql
SHOW TABLES;
SELECT username FROM t_user;
```

## 后端配置

默认配置文件：

```text
backend/src/main/resources/db.properties
```

内容：

```properties
db.url=jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
db.username=mall
db.password=mall
redis.host=localhost
redis.port=6379
redis.password=
```

推荐不要把真实密码写入 Git。Tomcat / IDEA Run Configuration 中配置环境变量覆盖：

```text
DB_USERNAME=mall;DB_PASSWORD=<本机数据库密码>;REDIS_HOST=localhost;REDIS_PORT=6379;REDIS_PASSWORD=
```

如果需要完整覆盖连接：

```text
DB_URL=jdbc:mysql://localhost:3306/mall?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true;DB_USERNAME=mall;DB_PASSWORD=<本机数据库密码>;REDIS_HOST=localhost;REDIS_PORT=6379;REDIS_PASSWORD=
```

## 启动方式

### IDEA + Tomcat

1. 用 IntelliJ IDEA 打开项目根目录。
2. 确认本机 MySQL 已启动，数据库 `mall` 已初始化。
3. 确认 Redis 已启动，`redis-cli ping` 返回 `PONG`。
4. 配置 Tomcat 10+。
5. 在 Deployment 中添加 `backend:war exploded`。
6. 在 Tomcat Run Configuration 中设置数据库和 Redis 环境变量。
7. 启动 Tomcat。
8. 浏览器访问：

```text
http://localhost:8080/mall-webapp/
```

如果 IDEA 部署上下文不是 `mall-webapp`，以 IDEA Deployment 中显示的 Application context 为准。

### Maven 打包

本机安装 Maven 后：

```powershell
cd backend
mvn clean package
```

生成：

```text
backend/target/mall-webapp.war
```

将 WAR 放入 Tomcat `webapps` 目录即可部署。

## 页面说明

当前前端是 `backend/src/main/webapp/index.html` 中的 jQuery 单页应用：

- `#/`：首页
- `#/products`：商品列表
- `#/products/{id}`：商品详情
- `#/cart`：购物车
- `#/checkout`：结算
- `#/orders`：我的订单
- `#/orders/{id}`：订单详情
- `#/admin/products`：商品管理
- `#/admin/categories`：分类管理
- `#/admin/ads`：广告管理
- `#/admin/orders`：订单管理
- `#/admin/users`：用户管理

## 接口说明

统一 Servlet：

```text
backend/src/main/java/com/mall/web/servlet/MallServlet.java
```

接口前缀：

```text
/api
```

主要接口：

| 模块 | 接口 |
|---|---|
| 用户 | `/api/users/login`、`/api/users/register`、`/api/users/logout` |
| 商品 | `/api/products`、`/api/products/{id}` |
| 分类 | `/api/categories` |
| 广告 | `/api/ads` |
| 购物车 | `/api/cart`、`/api/cart/{id}` |
| 订单 | `/api/orders`、`/api/orders/checkout`、`/api/orders/{id}` |
| 后台商品 | `/api/admin/products` |
| 后台分类 | `/api/admin/categories` |
| 后台广告 | `/api/admin/ads` |
| 后台订单 | `/api/admin/orders` |
| 后台用户 | `/api/admin/users` |

## 常见问题

### 登录失败

确认：

- `t_user` 表存在 `admin`
- Tomcat 环境变量中的 `DB_USERNAME`、`DB_PASSWORD` 正确
- MySQL 监听 `localhost:3306`

### 中文乱码

数据库初始化和导入必须使用 `utf8mb4`。手动导入时必须带：

```text
--default-character-set=utf8mb4
```

### jQuery 无法加载

当前页面通过 CDN 加载 jQuery：

```text
https://code.jquery.com/jquery-3.7.1.min.js
```

如果演示环境不能联网，需要下载 `jquery-3.7.1.min.js` 放到 `backend/src/main/webapp/assets/js/`，并修改 `index.html` 中的脚本地址。

### 回退到重写前版本

重写前已打标签：

```text
pre-webapp-rewrite-20260630
```

如需回退：

```powershell
git reset --hard pre-webapp-rewrite-20260630
```
