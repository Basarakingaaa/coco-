# 本次 Codex 会话交接记录

> 说明：本文记录本次会话中完成的项目迁移、配置调整、启动排障和答辩文档整理。  
> 安全处理：会话中曾出现邮箱 SMTP 授权码，本文不会写入真实授权码，只记录“需要重新生成并配置授权码”。不要把真实密钥提交到仓库。

## 1. 用户目标

用户最初要求：

- 将原项目从云原生/K8s 部署方式改为 Windows 本地部署。
- 希望通过 IntelliJ IDEA 展示和启动项目。
- 删除无用独立文档，把解释性说明统一写入 `README.md`。
- 保留邮箱验证码功能，并说明所需环境变量。
- 实际启动一次项目，修复本地启动问题。
- 修复前端中文乱码。
- 总结部署方法，更新 README。
- 增加答辩草稿和可能被问到的问题。
- 推送到 GitHub 仓库 `Basarakingaaa/coco-` 的新分支。
- 最后要求把本次会话记忆也写入分支。

## 2. 当前远程分支状态

本地目录原本不是 Git 仓库，因此本次在当前项目目录中重新初始化 Git，并推送到远程新分支。

- 远程仓库：`https://github.com/Basarakingaaa/coco-`
- 当前分支：`windows-local-deploy`
- 已推送提交：`0fcd8fa Convert project to Windows local deployment`
- GitHub PR 链接：`https://github.com/Basarakingaaa/coco-/pull/new/windows-local-deploy`

后续如果继续提交，应继续使用当前分支：

```powershell
git branch --show-current
git status
git add .
git commit -m "..."
git push
```

## 3. 项目定位变化

原项目包含 K8s、APISIX、cert-manager、NetworkPolicy、HPA 等云原生部署内容。用户明确要求“直接把整个项目改为 Windows 本地部署”，因此项目定位调整为：

- Windows 本地部署
- Docker Desktop 启动基础依赖
- IntelliJ IDEA 启动后端和前端
- 不再维护 K8s/APISIX/远程服务器部署路径

当前 README 已按 Windows 本地部署重新组织。

## 4. 当前推荐启动方式

推荐方式：Docker 只启动基础依赖，IDEA 启动源码。

启动顺序：

1. 打开 Docker Desktop。
2. 用 IDEA 打开项目根目录。
3. 在 IDEA Terminal 执行：

```powershell
.\scripts\start-local-deps.ps1
```

4. IDEA 运行后端主类：

```text
backend/src/main/java/com/mall/MallApplication.java
```

5. IDEA Terminal 启动前端：

```powershell
cd frontend
npm install
npm run dev
```

6. 浏览器访问：

```text
http://localhost:5173
```

默认管理员账号：

```text
admin / admin123
```

## 5. Docker Desktop 虚拟化问题

启动 Docker Desktop 时曾出现：

```text
Virtualization support not detected
```

处理过程：

- 确认 WSL 已安装。
- 执行 `wsl --set-default-version 2` 成功。
- 执行 Windows 功能启用命令：

```powershell
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All
Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All
bcdedit /set hypervisorlaunchtype auto
```

用户曾把命令误写成：

```powershell
bcdedit /set hypervisorlaunchtype autowsl --status
```

正确命令应拆开：

```powershell
bcdedit /set hypervisorlaunchtype auto
wsl --status
```

之后 Docker 可执行，项目依赖容器成功启动。

## 6. Docker Compose 配置修复

最初 `docker-compose.yml` 使用了不兼容的变量默认值写法：

```text
${DB_USERNAME:mall}
```

Docker Compose 报错：

```text
invalid interpolation format
```

修复为标准写法：

```text
${DB_USERNAME:-mall}
```

同类变量均已修复。

## 7. MySQL 端口冲突处理

启动 MySQL 容器时，`3306` 被本机已有 `mysqld` 占用。

排查结果：

```text
PID 6208 mysqld 占用 3306 / 33060
```

处理方式：

- Docker MySQL 外部端口改为 `3307`。
- 容器内部仍是 `3306`。
- 后端默认 JDBC 地址改为 `localhost:3307`。

关键文件：

- `docker-compose.yml`
- `.env.example`
- `backend/src/main/resources/application.yml`
- `README.md`
- `AGENTS.md`

当前默认连接：

```text
jdbc:mysql://localhost:3307/mall
```

## 8. JDBC 编码参数修复

后端启动曾报错：

```text
Unsupported character encoding 'utf8mb4'
```

原因：

- `utf8mb4` 是 MySQL 字符集名。
- JDBC URL 的 `characterEncoding` 参数应该使用 Java 支持的编码名。

修复：

```text
characterEncoding=UTF-8
```

数据库和表仍然使用 `utf8mb4`。

关键文件：

```text
backend/src/main/resources/application.yml
```

## 9. 中文乱码修复

前端页面曾显示类似：

```text
æ— çº¿...
```

排查结果：

- `sql/init.sql` 文件中的中文是正常的。
- MySQL 当前数据中中文已经变成乱码，说明旧数据卷曾用错误客户端编码导入过。

修复步骤：

1. 将 `sql/init.sql` 复制到 MySQL 容器内。
2. 使用 `mysql --default-character-set=utf8mb4` 重新导入。
3. 清空 Redis 缓存。
4. 验证后端接口返回正常中文。

为了防止复发，新增：

```text
sql/init-local.sh
```

内容：

```sh
#!/bin/sh
set -e

mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" < /init.sql
```

并修改 `docker-compose.yml`，让 MySQL 初始化时通过该脚本导入 SQL。

如果未来再次出现乱码，执行：

```powershell
.\scripts\stop-local.ps1 -WithData
.\scripts\start-local-deps.ps1
```

## 10. Maven / IDEA 依赖问题

IDEA 中曾出现 Lombok、Spring 依赖无法解析：

```text
程序包 lombok 不存在
未解析的依赖项 org.projectlombok:lombok
```

处理建议：

1. 右键 `backend/pom.xml`，选择添加为 Maven 项目。
2. IDEA Maven 设置中勾选用户设置文件重写。
3. 将 Maven User settings file 设置为：

```text
backend/settings.xml
```

该文件配置了阿里云 Maven 镜像：

```xml
https://maven.aliyun.com/repository/public
```

4. Reload All Maven Projects。
5. 安装或启用 Lombok 插件。
6. 启用 Annotation Processing。

命令行验证：

```powershell
cd backend
mvn -s settings.xml dependency:resolve
```

## 11. 邮箱验证码配置和问题

邮箱验证码功能仍然保留，需要配置 SMTP。

后端读取这些环境变量：

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

QQ 邮箱示例：

```text
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=完整 QQ 邮箱地址
MAIL_PASSWORD=SMTP 授权码
```

重要说明：

- `MAIL_PASSWORD` 不是邮箱登录密码。
- 它是邮箱后台生成的 SMTP 授权码。
- 如果使用 IDEA 直接运行后端，Spring Boot 不会自动读取项目根目录 `.env`。
- 需要在 IDEA Run Configuration 的 `Environment variables` 配置，或设置 Windows 用户环境变量。

PowerShell 设置方式：

```powershell
[Environment]::SetEnvironmentVariable("MAIL_HOST", "smtp.qq.com", "User")
[Environment]::SetEnvironmentVariable("MAIL_PORT", "587", "User")
[Environment]::SetEnvironmentVariable("MAIL_USERNAME", "你的QQ邮箱", "User")
[Environment]::SetEnvironmentVariable("MAIL_PASSWORD", "你的SMTP授权码", "User")
```

设置后必须完全关闭并重新打开 IDEA。

会话中曾出现 QQ SMTP 授权码。该授权码已经暴露，建议用户立刻在 QQ 邮箱后台撤销并重新生成。本文和仓库不会保存真实授权码。

如果 QQ 返回：

```text
535 Login fail
```

常见原因：

- SMTP 服务未开启。
- 授权码错误或已失效。
- 账号异常。
- 登录频率限制。

## 12. IDEA 运行配置注意点

用户曾误把邮箱环境变量填入 IDEA 的“有效配置文件”字段。

正确说明：

- “有效配置文件”是 Spring Profiles，不是环境变量。
- 本项目后端现在不需要设置 Spring Profile。
- 邮箱变量应放在 Environment variables。

如果 IDEA UI 找不到环境变量入口，建议使用 Windows 用户环境变量方式。

## 13. 文件夹 `(4)` 复制情况

用户曾要求把项目复制到：

```text
C:\Users\Administrator\Desktop\新建文件夹 (4)
```

已复制并清理生成目录：

- `backend/target`
- `frontend/node_modules`
- `frontend/dist`

但后续用户明确要求“不再同步到文件夹 4，只需要推送到远程仓库新分支”，因此后续变更只应在当前仓库分支中处理，不再主动同步 `(4)`。

## 14. README 更新内容

当前 `README.md` 已重写为详细 Windows 本地部署说明，包含：

- 功能概览
- 本地架构
- 技术栈
- 目录结构
- 部署前准备
- IDEA 展示模式
- 完整 Docker Compose 模式
- 环境变量说明
- 启动后验证
- 常用维护命令
- 常见问题

重点说明：

- MySQL 使用 `3307`
- JDBC 使用 `characterEncoding=UTF-8`
- 数据库导入使用 `utf8mb4`
- 邮箱验证码环境变量配置
- Docker Desktop 虚拟化问题处理
- Maven 依赖解析问题处理

## 15. 答辩草稿

新增：

```text
答辩草稿.md
```

内容包括：

- 项目介绍稿
- 演示流程稿
- 系统架构说明
- 核心功能说明
- 关键技术点
- 可能被问到的问题
- 答辩结束语

常见答辩问题覆盖：

- 为什么本地部署
- 前后端如何交互
- JWT 鉴权如何做
- Redis 为什么使用
- MinIO 为什么使用
- 下单库存如何处理
- 邮箱验证码如何排查
- 中文乱码原因
- `utf8mb4` 和 `UTF-8` 的区别
- 项目不足和后续优化方向

## 16. 当前关键文件

```text
README.md
答辩草稿.md
docker-compose.yml
.env.example
backend/src/main/resources/application.yml
scripts/start-local-deps.ps1
scripts/start-local.ps1
scripts/stop-local.ps1
sql/init.sql
sql/init-local.sh
AGENTS.md
SESSION_MEMORY.md
```

## 17. 当前运行验证结果

本次会话中已验证：

- Docker Desktop 可运行。
- MySQL、Redis、MinIO 容器可启动。
- Redis 和 MinIO healthy。
- MySQL 使用 `3307` 规避端口冲突。
- MySQL 初始化表存在。
- `admin` 用户存在。
- 后端接口 `/api/products` 返回正常中文。
- Redis 缓存已清理过。

## 18. 安全注意事项

不要提交：

- `.env`
- SMTP 授权码
- 邮箱真实授权码
- Docker Desktop 本地凭据
- IDEA `.idea` 本地配置
- `frontend/node_modules`
- `backend/target`

本次仓库中 `.gitignore` 已忽略 `.env`、`node_modules`、`dist`、`target` 等本地文件。

再次提醒：会话中曾暴露 SMTP 授权码，建议立即撤销并重新生成。
