# XX 电子商务系统（K8s Mall）

一个跑在轻量级 K3s 集群上的电商系统：React 前端 + Spring Boot 后端 + MySQL/Redis + APISIX 网关，覆盖用户下单全流程和基础的后台管理能力。

## 目录

- [功能架构](#功能架构)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [部署到远程服务器](#部署到远程服务器)
- [实际部署记录与问题排查](#实际部署记录与问题排查)
- [常见问题](#常见问题)

---

## 功能架构

```
XX电子商务系统
├── 用户端
│   ├── 首页（轮播广告 + 分类导航 + 推荐商品）
│   ├── 商品列表（分类筛选 / 关键词搜索）
│   ├── 商品详情（SKU 选择、规格参数、图文详情）
│   ├── 购物车（勾选结算、数量调整）
│   ├── 注册 / 登录（邮箱验证码校验，注册成功自动登录）
│   └── 结算下单（生成订单、扣减库存）
└── 管理端（网页 UI，登录后访问 /admin）
    ├── 商品管理（含主图/主视频上传，上传到自建 MinIO 对象存储）
    ├── 分类管理
    ├── 广告管理（含广告类别）
    ├── 订单管理（查看 + 修改订单状态）
    └── 用户管理（查看 + 启用/禁用账号）
```

管理端登录入口与普通用户共用 `/login`，登录账号的角色为 `ADMIN` 时会自动跳转到 `/admin`；普通用户访问 `/admin` 会被重定向回登录页。数据库初始化脚本（[sql/init.sql](sql/init.sql)）已内置一个默认管理员账号：

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | **务必在生产环境部署后立即登录修改密码**（后台暂未提供改密功能，需直接更新数据库 `t_user` 表的 `password` 字段，存的是 BCrypt 哈希） |

核心业务流程：

```
浏览商品 → 加入购物车 → 勾选结算 → 填写收货信息 → 提交订单
                                          ↓
                              后端事务内扣减 SKU/商品库存
                                          ↓
                              生成订单号，订单状态 = 待支付
```

---

## 系统架构

```
用户浏览器
    ↓ HTTPS / TLS
Gateway API（APISIX）
├── WAF 防护（ModSecurity 规则）
├── 限流 / API Key 鉴权
└── 路由规则（/api → 后端，/ → 前端）
    ↓
┌─────────────────────────────────────────────────────┐
│                   K3s Cluster                        │
│                                                      │
│  ┌─ Service：前端 ──────────────────────────────┐   │
│  │  前端 Pod（React+Nginx）× N   ← HPA 扩容     │   │
│  └──────────────────────────────────────────────┘   │
│                  ↓ Network Policy 放行               │
│  ┌─ Service：后端 ──────────────────────────────┐   │
│  │  后端 Pod（Spring Boot+JWT）× N ← HPA 伸缩   │   │
│  │           CPU 阈值 50% 触发扩容               │   │
│  └──────────────────────────────────────────────┘   │
│            ↓              ↓              ↓          │
│  ┌─ MySQL ─┐  ┌─ Redis ──┐  ┌─ MinIO ─────────┐    │
│  │ Pod × 1 │  │ Pod × 1  │  │ Pod × 1         │    │
│  │ + PVC   │  │ 缓存层   │  │ + PVC 对象存储  │    │
│  └─────────┘  └──────────┘  │ 商品图片/视频   │    │
│                              └─────────────────┘    │
│                                                      │
│  Secret → DB密码 / JWT密钥 / 邮箱SMTP / MinIO密钥 / TLS证书 │
│  Pod Security Standard（restricted）→ 禁止特权容器  │
│  HPA + metrics-server → 自动监控CPU/内存指标        │
│  cert-manager → TLS证书自动续期                     │
│  Network Policy → 只允许前端Pod访问后端Pod          │
└─────────────────────────────────────────────────────┘
```

请求链路：`用户 → APISIX(WAF/限流/路由) → 前端Pod(静态资源) / 后端Pod(/api)` ；后端再访问 MySQL（持久数据）、Redis（商品详情缓存、注册验证码）与 MinIO（管理后台上传的商品图片/视频，S3协议兼容的自建对象存储）。

---

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | React + Vite + React Router，Apple 风格 UI |
| 后端 | Spring Boot 3 + Spring Security（JWT）+ Spring Data JPA |
| 数据库 | MySQL 8（持久数据）+ Redis 7（缓存 / 验证码） |
| 对象存储 | MinIO（S3协议兼容，自建，存放商品图片/视频） |
| 邮件 | Spring Mail（注册邮箱验证码） |
| 集群 | K3s（轻量 Kubernetes） |
| 网关 | APISIX（路由 + WAF + 限流） |
| TLS | cert-manager 自动续期 |
| 伸缩 | HPA（CPU 阈值 50%） |
| 安全 | NetworkPolicy + Pod Security Standard（restricted）+ Secret |

---

## 仓库结构

```
coco-/                 # 仓库地址：https://github.com/Basarakingaaa/coco-
├── frontend/          # React 前端
├── backend/           # Spring Boot 后端
├── k8s/               # 所有 k8s 部署清单
├── sql/init.sql       # 数据库初始化脚本
├── scripts/           # build.sh / deploy.sh / load-test.sh
└── README.md
```

---

## 部署到远程服务器

以下步骤以实际部署使用的**境内云服务器（腾讯云CVM，2核4GB，Ubuntu 22.04）**为基准，与通用教程的最大区别是：**境内服务器访问 Docker Hub / GitHub Container Registry（ghcr.io）/ quay.io 等境外资源大概率会连接超时或被重置**，因此每一步都给出了对应的国内可用替代方案。如果你的服务器在境外（如AWS/GCP海外区域），可以直接用官方安装脚本，跳过镜像加速相关步骤。

### 第一步：服务器基础环境

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl git
sudo reboot   # 如果升级到了新内核，重启一次更稳妥
```

### 第二步：安装 Docker

> **境内服务器请勿用官方脚本**——`curl -fsSL https://get.docker.com | sh` 在境内大概率会报 `OpenSSL SSL_connect: Connection reset by peer`。改用系统自带源：

```bash
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $(whoami)
# 退出重新登录SSH让用户组生效，再验证：
docker version
```

### 第三步：安装 K3s

> 官方脚本 `get.k3s.io` 本身可达，但它默认会去 Docker Hub 拉取 `rancher/mirrored-pause` 等基础镜像，境内同样会卡住。**装的时候就用 Rancher 官方的中国镜像源**，从源头避免后面排障：

```bash
curl -sfL https://rancher-mirror.rancher.cn/k3s/k3s-install.sh | INSTALL_K3S_MIRROR=cn sh -

mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
export KUBECONFIG=~/.kube/config

kubectl get nodes   # 确认节点 Ready
```

### 第四步：配置容器镜像加速（关键步骤，避免后续所有 `ImagePullBackOff`）

K3s 装好后，**立刻**配置containerd的镜像加速，否则装 cert-manager / APISIX 时几乎必现镜像拉取超时：

```bash
sudo mkdir -p /etc/rancher/k3s
sudo tee /etc/rancher/k3s/registries.yaml <<'EOF'
mirrors:
  docker.io:
    endpoint:
      - "https://docker.m.daocloud.io"
  quay.io:
    endpoint:
      - "https://quay.m.daocloud.io"
  ghcr.io:
    endpoint:
      - "https://ghcr.m.daocloud.io"
EOF
sudo systemctl restart k3s
```

后面如果要在服务器上用 `docker build`/`docker pull`（独立于K3s的Docker daemon），**还要单独给Docker daemon也配一份**，否则会复现同样的问题：

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<'EOF'
{ "registry-mirrors": ["https://docker.m.daocloud.io"] }
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
docker info | grep -A3 "Registry Mirrors"   # 确认生效
```

### 第五步：禁用K3s自带的Traefik

K3s默认内置Traefik作为网关，会跟后面要装的APISIX在Gateway API的CRD上冲突，**必须先禁用**：

```bash
sudo mkdir -p /etc/rancher/k3s
sudo tee -a /etc/rancher/k3s/config.yaml <<'EOF'
disable:
  - traefik
EOF
sudo systemctl restart k3s

# 清理Traefik遗留的Gateway API CRD（不清理APISIX装不上）
kubectl delete crd \
  backendtlspolicies.gateway.networking.k8s.io \
  gatewayclasses.gateway.networking.k8s.io \
  gateways.gateway.networking.k8s.io \
  grpcroutes.gateway.networking.k8s.io \
  httproutes.gateway.networking.k8s.io \
  listenersets.gateway.networking.k8s.io \
  referencegrants.gateway.networking.k8s.io \
  tcproutes.gateway.networking.k8s.io \
  tlsroutes.gateway.networking.k8s.io \
  udproutes.gateway.networking.k8s.io \
  --ignore-not-found
```

### 第六步：安装Helm

```bash
sudo snap install helm --classic
helm version
```

### 第七步：安装 cert-manager

```bash
helm repo add jetstack https://charts.jetstack.io --force-update
helm repo update
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set crds.enabled=true

kubectl get pods -n cert-manager   # 等三个 Pod 都 Running（依赖第四步的镜像加速）
```

> **没有完成ICP备案的域名无法用Let's Encrypt签发公网可信证书**（Let's Encrypt需要从公网验证域名归属）。本项目改用 `SelfSigned` 类型的 `ClusterIssuer`（见 [k8s/certificate.yaml](k8s/certificate.yaml)），配合一个只在本地hosts文件里映射的测试域名 `mall.test`（IANA保留的`.test`顶级域，永远不会出现在公网DNS里，因此也不会触发备案监管），完成HTTPS加密链路搭建。浏览器访问时会提示"证书不受信任"，点击"继续访问"即可，传输内容依然是加密的。如果你的域名已完成备案，可以把 `ClusterIssuer` 换回ACME类型走Let's Encrypt签发可信证书。

### 第八步：安装APISIX网关

```bash
helm repo add apisix https://charts.apiseven.com --force-update
helm repo update
kubectl create namespace apisix

# 注意：默认的 ingress-controller 2.x 带一个 adc-server 子容器，
# 镜像在 ghcr.io/api7/adc，即使配置了镜像加速也经常拉取很慢/403，
# 这里先禁用 ingress-controller，并把网关Service类型设为 LoadBalancer
# （K3s自带ServiceLB会自动把80/443绑定到节点网卡，访问不用带端口号）
helm install apisix apisix/apisix \
  --namespace apisix \
  --set ingress-controller.enabled=false \
  --set etcd.replicaCount=1 \
  --set apisix.admin.allow.ipList[0]="0.0.0.0/0" \
  --set apisix.ssl.enabled=true \
  --set service.type=LoadBalancer

kubectl get pods -n apisix
kubectl get svc -n apisix apisix-gateway   # 确认 EXTERNAL-IP 已分配，PORT(S) 含 80/443
```

> `etcd.replicaCount=1` 是因为单节点小内存机器（2核4GB）跑不起默认的3副本etcd；`apisix.admin.allow.ipList` 默认只放行 `127.0.0.1`，必须显式覆盖才能让其他组件调用Admin API。**路由规则不再通过Kubernetes CRD声明式管理**（旧版`apisix-ingress-controller`跟新版APISIX的Admin API在SSL资源同步上有兼容性问题），改为直接调APISIX的Admin API创建路由，具体命令见下方[实际部署记录](#实际部署记录与问题排查)。

### 第九步：拉取代码到服务器

```bash
git clone https://github.com/Basarakingaaa/coco-.git
cd coco-
```

### 第十步：构建镜像并导入K3s

```bash
docker build -t mall/backend:latest ./backend
docker build -t mall/frontend:latest ./frontend
docker save mall/backend:latest mall/frontend:latest -o mall-images.tar
sudo k3s ctr images import mall-images.tar
rm -f mall-images.tar   # 导入完删掉，省磁盘空间
```

把 [k8s/backend-deploy.yaml](k8s/backend-deploy.yaml) 和 [k8s/frontend-deploy.yaml](k8s/frontend-deploy.yaml) 里的 `image` 字段改成 `mall/backend:latest` / `mall/frontend:latest`，`imagePullPolicy` 设为 `IfNotPresent`（这两个文件里的镜像地址是面向真实镜像仓库的占位值，单机部署直接本地导入不需要仓库）。

> **改完代码重新构建时，如果发现报错内容跟修复前完全一样**，大概率是Docker复用了旧的构建缓存（哪怕`pom.xml`/源码确实改了），用 `docker build --no-cache -t mall/backend:latest ./backend` 强制完全重新编译。

### 第十一步：准备 Secret（数据库密码 / JWT 密钥 / 邮箱 SMTP / MinIO 密钥）

**不要**把真实密钥提交到 git。在服务器上执行（先创建命名空间）：

```bash
kubectl apply -f k8s/namespace.yaml

kubectl create secret generic mysql-secret -n mall \
  --from-literal=MYSQL_ROOT_PASSWORD='设置一个强密码' \
  --from-literal=MYSQL_DATABASE='mall' \
  --from-literal=MYSQL_USER='mall' \
  --from-literal=MYSQL_PASSWORD='设置一个强密码'

# MinIO 自身的账号密码（即 root user/password，给 MinIO 容器自己用）
kubectl create secret generic minio-secret -n mall \
  --from-literal=MINIO_ROOT_USER='admin' \
  --from-literal=MINIO_ROOT_PASSWORD="$(openssl rand -base64 16)"

kubectl create secret generic backend-secret -n mall \
  --from-literal=DB_USERNAME='mall' \
  --from-literal=DB_PASSWORD='跟上面 MYSQL_PASSWORD 保持一致' \
  --from-literal=REDIS_PASSWORD='' \
  --from-literal=JWT_SECRET="$(openssl rand -base64 32)" \
  --from-literal=MAIL_HOST='smtp.qq.com' \
  --from-literal=MAIL_PORT='587' \
  --from-literal=MAIL_USERNAME='你的发信邮箱@qq.com' \
  --from-literal=MAIL_PASSWORD='邮箱后台申请到的SMTP授权码（不是登录密码）' \
  --from-literal=MINIO_ENDPOINT='http://minio:9000' \
  --from-literal=MINIO_ACCESS_KEY='跟上面 MINIO_ROOT_USER 保持一致' \
  --from-literal=MINIO_SECRET_KEY='跟上面 MINIO_ROOT_PASSWORD 保持一致' \
  --from-literal=MINIO_BUCKET='mall-media' \
  --from-literal=MINIO_PUBLIC_URL='http://<服务器公网IP>:30682'
```

邮箱 SMTP 授权码获取方式（任选一个邮箱服务商）：
- QQ 邮箱：登录网页版 → 设置 → 账户 → 开启「POP3/IMAP/SMTP服务」→ 生成授权码
- 163 邮箱：设置 → POP3/SMTP/IMAP → 开启服务 → 生成授权码
- Gmail：账号需开启两步验证后，在「应用专用密码」里生成

> 如果没有备案域名，`MINIO_PUBLIC_URL` 可以先填 `http://<服务器公网IP>:30682`，配合下面第十二步把MinIO的Service类型改成NodePort并固定该端口；备案完成、有了真实域名之后再切回域名形式，并在腾讯云安全组里放行对应端口。

### 第十二步：部署 ConfigMap、数据库、缓存、对象存储

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/mysql-deploy.yaml
kubectl apply -f k8s/redis-deploy.yaml
kubectl apply -f k8s/minio-deploy.yaml   # Service 已配置为 NodePort 30682(API)/30683(控制台)

kubectl get pods -n mall   # 等 mysql / redis / minio 都 Running
```

### 第十三步：初始化数据库表结构

> **执行`mysql`命令行客户端时一定要显式指定UTF-8编码**，否则种子数据里的中文会在写入时就被错误编码，存进数据库的就是乱码（事后用Nginx/Spring的charset配置补救不了，因为坑在数据本身已经写错了）：

```bash
MYSQL_POD=$(kubectl get pod -n mall -l app=mysql -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n mall "${MYSQL_POD}" -i -- \
  sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/init.sql

# 验证表和中文种子数据都正常
kubectl exec -n mall "${MYSQL_POD}" -i -- sh -c \
  'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "USE mall; SELECT name FROM t_product_category;"'
```

### 第十四步：部署后端 / 前端

```bash
kubectl apply -f k8s/backend-deploy.yaml
kubectl apply -f k8s/frontend-deploy.yaml
kubectl rollout status deployment/backend -n mall --timeout=180s
kubectl rollout status deployment/frontend -n mall --timeout=120s
```

### 第十五步：应用 HPA 与 NetworkPolicy

```bash
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/networkpolicy.yaml
```

> **不要给NetworkPolicy加任何`policyTypes: Egress`的策略**，哪怕只是想"顺手放行DNS"。Ingress型的限制不会影响Pod自身的出站连接，DNS解析默认就是通的；一旦有Egress型策略选中了某个Pod（即使`podSelector: {}`选的是全部Pod），那个Pod的出站流量就会从"默认放行"变成"默认拒绝、只放行策略里列出的端口"，本仓库的 [k8s/networkpolicy.yaml](k8s/networkpolicy.yaml) 已经踩过这个坑并改好了，不要在此基础上再加Egress策略。

### 第十六步：配置网关路由（域名 + TLS）

把 [k8s/certificate.yaml](k8s/certificate.yaml)、[k8s/gateway.yaml](k8s/gateway.yaml) 里的 `mall.test` 换成你自己的域名（已备案的真实域名，或者继续用`.test`保留中测试模式），然后：

```bash
kubectl apply -f k8s/certificate.yaml
kubectl apply -f k8s/gateway.yaml
```

> 如果Helm装的APISIX用的是新版`ingress-controller`（带`adc-server`子容器），上面这两个CRD资源大概率不会被真正同步到APISIX（详见[实际部署记录](#实际部署记录与问题排查)里的具体问题）。本仓库的部署方式是**禁用了ingress-controller，直接调用APISIX的Admin API手动创建路由和SSL证书**，命令如下（`edd1c9f034335f136f87ad84b625c8f1`是APISIX chart的默认admin key，生产环境建议改成自己生成的）：

```bash
kubectl port-forward -n apisix svc/apisix-admin 9180:9180 &

# 域名路由（按 Host 头匹配，用于演示"网关绑定自定义域名"）
curl -s -X POST http://127.0.0.1:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
  -d '{"uri":"/*","host":"mall.test","upstream":{"type":"roundrobin","nodes":{"frontend.mall.svc.cluster.local:80":1}}}'
curl -s -X POST http://127.0.0.1:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
  -d '{"uri":"/api/*","host":"mall.test","upstream":{"type":"roundrobin","nodes":{"backend.mall.svc.cluster.local:8080":1}}}'

# 不限Host的兜底路由（用于纯IP公开访问）
curl -s -X POST http://127.0.0.1:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
  -d '{"uri":"/api/*","upstream":{"type":"roundrobin","nodes":{"backend.mall.svc.cluster.local:8080":1}}}'
curl -s -X POST http://127.0.0.1:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
  -d '{"uri":"/*","upstream":{"type":"roundrobin","nodes":{"frontend.mall.svc.cluster.local:80":1}}}'

kill %1   # 关掉端口转发
```

### 第十七步：域名与本机解析（没有备案域名时）

由于没有完成ICP备案，无法把 `mall.test` 这样的私有测试域名挂到真实公网DNS上（挂了也会被云厂商拦截）。改用**本机hosts文件映射**，只有手动加过映射的设备才能用域名访问，不影响纯IP的公开访问：

```
# Windows: 以管理员身份编辑 C:\Windows\System32\drivers\etc\hosts
# Linux/Mac: sudo编辑 /etc/hosts
<服务器公网IP> mall.test
```

如果域名已完成备案，直接把 `mall.test` 替换成真实域名、DNS A记录指向服务器公网IP，并把 [k8s/certificate.yaml](k8s/certificate.yaml) 里的 `ClusterIssuer` 换回ACME类型即可走Let's Encrypt签发可信证书。

### 第十八步：验证部署

```bash
kubectl get pods -n mall          # 所有 Pod 应为 Running
kubectl get hpa -n mall           # 查看 HPA 当前副本数/CPU使用率
kubectl get certificate -n mall   # 等待 READY=True

kubectl get svc -n apisix apisix-gateway   # 确认 LoadBalancer 已分配 EXTERNAL-IP，80/443 已映射

curl -I http://<服务器公网IP>/                              # 纯IP访问，前端
curl -I http://<服务器公网IP>/api/products                  # 纯IP访问，后端API
curl -I -H "Host: mall.test" http://<服务器公网IP>/         # 域名访问（命令行模拟Host头）
```

部署完成后用浏览器访问 `http://<服务器公网IP>/login`（或者改过hosts后访问 `https://mall.test/login`），用 `admin`/`admin123` 登录会自动跳转到后台管理页 `/admin`，可以在「商品管理」里新增商品并直接上传图片/视频——上传接口会把文件存进 MinIO，数据库只保存返回的 URL。

### 压测验证自动伸缩（可选）

```bash
TARGET_URL=http://<服务器公网IP>/api/products ./scripts/load-test.sh
```

---

## 实际部署记录与问题排查

这一节记录的是**真实部署到腾讯云CVM（2核4GB，Ubuntu 22.04）时按时间顺序遇到的问题**，每一条都附上了当时在服务器上敲的具体命令。如果你的部署过程中遇到类似报错，可以直接对照排查；如果一切顺利，可以跳过这一节。

### 1. Docker / K3s 官方安装脚本被墙

**现象**：`curl -fsSL https://get.docker.com | sh` 报 `curl: (35) OpenSSL SSL_connect: Connection reset by peer`。

**原因**：境内网络访问 Docker Inc. 的官方基础设施不稳定。

**服务器操作**：
```bash
sudo apt install -y docker.io
sudo systemctl enable --now docker

curl -sfL https://rancher-mirror.rancher.cn/k3s/k3s-install.sh | INSTALL_K3S_MIRROR=cn sh -
```

### 2. K3s拉取基础镜像卡死（ContainerCreating 长时间不变化）

**现象**：装完cert-manager/APISIX后，Pod 一直停在 `ContainerCreating`，`kubectl describe pod` 显示 `failed to pull image "rancher/mirrored-pause:3.6" ... dial tcp ... i/o timeout`。

**原因**：K3s用到的基础沙箱镜像（pause镜像）默认从 Docker Hub 拉取，境内同样不通；后续cert-manager/APISIX自身的镜像也会遇到同样问题。

**服务器操作**：
```bash
sudo mkdir -p /etc/rancher/k3s
sudo tee /etc/rancher/k3s/registries.yaml <<'EOF'
mirrors:
  docker.io:
    endpoint: ["https://docker.m.daocloud.io"]
  quay.io:
    endpoint: ["https://quay.m.daocloud.io"]
  ghcr.io:
    endpoint: ["https://ghcr.m.daocloud.io"]
EOF
sudo systemctl restart k3s
kubectl delete pods -n <对应namespace> --all   # 配置生效后让卡住的Pod重新拉取
```

### 3. K3s自带Traefik与APISIX的Gateway API CRD冲突

**现象**：`helm install apisix ...` 报 `Error: INSTALLATION FAILED: failed to install CRD ... "backendtlspolicies.gateway.networking.k8s.io" is invalid: status.storedVersions[0] ...`。

**原因**：K3s默认内置Traefik，并自动装了一套Gateway API CRD，跟APISIX chart要装的版本冲突。

**服务器操作**：
```bash
sudo tee -a /etc/rancher/k3s/config.yaml <<'EOF'
disable:
  - traefik
EOF
sudo systemctl restart k3s

kubectl delete crd backendtlspolicies.gateway.networking.k8s.io \
  gatewayclasses.gateway.networking.k8s.io gateways.gateway.networking.k8s.io \
  grpcroutes.gateway.networking.k8s.io httproutes.gateway.networking.k8s.io \
  listenersets.gateway.networking.k8s.io referencegrants.gateway.networking.k8s.io \
  tcproutes.gateway.networking.k8s.io tlsroutes.gateway.networking.k8s.io \
  udproutes.gateway.networking.k8s.io --ignore-not-found
```

### 4. APISIX Ingress Controller的adc-server子容器拉取GHCR镜像失败

**现象**：`apisix-ingress-controller` Pod 卡在 `0/2 ContainerCreating`，`kubectl describe` 显示在拉 `ghcr.io/api7/adc:0.26.0` 时一直 `Pulling`，换镜像加速源后变成 `403 Forbidden`。

**原因**：新版（2.x）ingress-controller自带一个`adc-server`sidecar容器，镜像固定在ghcr.io，国内镜像代理对这个特定镜像支持不稳定。

**服务器操作**（绕开这个组件，改用不带该sidecar的旧版本，再发现旧版本跟新版APISIX的Admin API在SSL资源上不兼容后，最终改为完全禁用ingress-controller、手动用Admin API管理路由）：
```bash
helm upgrade apisix apisix/apisix -n apisix \
  --set ingress-controller.enabled=false \
  --set etcd.replicaCount=1 \
  --set apisix.admin.allow.ipList[0]="0.0.0.0/0" \
  --set apisix.ssl.enabled=true \
  --set service.type=LoadBalancer
```
后续路由通过Admin API直接创建（命令见[部署到远程服务器·第十六步](#部署到远程服务器)）。

### 5. APISIX Admin API默认只允许127.0.0.1访问，导致403

**现象**：`kubectl logs` 看ingress-controller或自己调Admin API时报 `403 Forbidden`（注意这是openresty返回的通用403页面，不是Admin API自己的鉴权错误）。

**原因**：APISIX chart默认的`apisix.admin.allow.ipList`只放行127.0.0.1，Pod间调用的源IP不在白名单内。

**服务器操作**：
```bash
helm upgrade apisix apisix/apisix -n apisix \
  --set apisix.admin.allow.ipList[0]="0.0.0.0/0" \
  --reuse-values
kubectl rollout restart deployment/apisix -n apisix
```

### 6. 单节点2核4GB资源不够，HPA扩容后CPU打满

**现象**：`kubectl get hpa` 显示 `cpu: 332%/50%`，疯狂扩容到3个副本，新副本反而连续重启。

**原因**：默认的资源配置（`requests`/`limits`）和APISIX自带etcd的3副本，对2核4GB的节点偏重。

**服务器操作**：调小 [k8s/backend-deploy.yaml](k8s/backend-deploy.yaml)、[k8s/frontend-deploy.yaml](k8s/frontend-deploy.yaml)、[k8s/hpa.yaml](k8s/hpa.yaml) 里的资源配置和副本数区间（已经在仓库里改好），并把APISIX的etcd缩到1副本：
```bash
helm upgrade apisix apisix/apisix -n apisix --set etcd.replicaCount=1 --reuse-values
```

### 7. NetworkPolicy里一条"顺手放行DNS"的策略，误伤了所有Pod的出站流量

**现象**：应用NetworkPolicy后，backend连MySQL报 `Caused by: java.net.ConnectException: Connection refused`，但MySQL本身是健康的。

**原因**：一条`policyTypes: [Egress]`、`podSelector: {}`、只放行53端口的策略，本意是"放行DNS"，但只要有Egress类型的策略选中了某个Pod，该Pod的所有出站流量就从"默认放行"变成"默认拒绝、仅放行策略列出的端口"——backend访问MySQL/Redis/MinIO的出站连接全被这条策略误伤。

**服务器操作**：
```bash
kubectl delete networkpolicy allow-dns -n mall
```
（已在仓库的 [k8s/networkpolicy.yaml](k8s/networkpolicy.yaml) 里彻底移除这条策略——Ingress限制不影响Pod自身出站，DNS解析本来就不需要额外放行）

### 8. 调试用的临时Pod忘记打安全标签之外的区分，被Service误收编

**现象**：解决了NetworkPolicy问题后，backend依然偶发502，`connect() failed (111: Connection refused)`。

**原因**：之前为了测连通性创建的`nettest`调试Pod，给它打了跟真实backend一样的`app=backend`标签，结果被backend的Service自动当成一个负载均衡节点，但这个Pod根本没监听8080端口。

**服务器操作**：
```bash
kubectl get endpoints -n mall backend   # 发现多了一个不该有的IP
kubectl delete pod -n mall nettest
```
> 经验：任何临时调试Pod，**不要复用业务Pod的标签**，避免被Service/NetworkPolicy当成真实成员。

### 9. 没有ICP备案，域名访问与HTTPS证书无法走标准路径

**现象**：想要"网关绑定自定义域名+HTTPS"，但服务器在境内，公网域名必须先完成ICP备案才能正常访问。

**服务器操作**：用IANA保留的`.test`测试域名（`mall.test`，永不会出现在公网DNS），TLS证书用cert-manager的`SelfSigned`类型`ClusterIssuer`自签：
```bash
kubectl apply -f k8s/certificate.yaml   # 内含 selfsigned-issuer + mall-tls Certificate
```
访问者在自己电脑的hosts文件里加一行 `<服务器IP> mall.test` 即可用域名+HTTPS访问，不影响纯IP的公开访问。

### 10. Docker构建缓存导致改了代码却不生效（出现两次）

**现象**：改完`pom.xml`/Java代码后重新`docker build`，构建日志显示 `Using cache`，部署后报错跟改之前一模一样。

**原因**：Docker按文件内容哈希判断是否复用缓存层，但在某些场景下（尤其是改动量很小、或者`git pull`实际没拉到最新代码时）会误判缓存有效。

**服务器操作**：
```bash
git pull origin main && git log --oneline -1   # 先确认确实拉到了预期的commit
docker build --no-cache -t mall/backend:latest ./backend   # 强制完全重新构建
```

### 11. Hibernate的`ddl-auto: validate`对字段类型校验极严格

**现象**：后端启动直接崩溃，连续报了几种"字段类型不匹配"——`status`字段（数据库TINYINT，Java`Integer`默认映射成INTEGER）、`detail_html`字段（数据库LONGTEXT，`@Lob`默认映射成TINYTEXT）、`media_type`字段（数据库VARCHAR，Hibernate6默认尝试用原生ENUM类型）。

**服务器操作**：这几处都是代码层修复（在对应`@Column`注解上显式声明`columnDefinition`），不是服务器配置问题，改完代码后照样要走「拉代码→`docker build --no-cache`→导入→`kubectl rollout restart`」的完整流程：
```bash
git pull origin main
docker build --no-cache -t mall/backend:latest ./backend
docker save mall/backend:latest -o mall-backend.tar
sudo k3s ctr images import mall-backend.tar
kubectl rollout restart deployment/backend -n mall
```

### 12. 数据库种子数据导入时编码错误，前端中文显示乱码

**现象**：前端页面里固定写死的文字（导航栏、页脚）显示正常，但从数据库读出来的商品名、分类名全部乱码（典型的"UTF-8字节被当Latin-1解析后又重新编码"的二次乱码）。

**原因**：最初执行 `sql/init.sql` 时，`mysql`命令行客户端没有用UTF-8解析文件内容，写入数据库的字节就已经是错的，不是页面渲染问题。

**服务器操作**：
```bash
MYSQL_POD=$(kubectl get pod -n mall -l app=mysql -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n mall "${MYSQL_POD}" -i -- sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"' <<'SQL'
DROP DATABASE IF EXISTS mall;
SQL
kubectl exec -n mall "${MYSQL_POD}" -i -- sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/init.sql
```

### 13. Redis缓存的ObjectMapper没注册时间模块，商品详情页卡死加载

**现象**：商品列表正常，点进详情页一直转圈，后端返回500：`Could not write JSON: Java 8 date/time type 'java.time.LocalDateTime' not supported by default`。

**原因**：`RedisConfig`里给Redis序列化单独`new`了一个`ObjectMapper`，跟Spring Boot自动配置给HTTP响应用的那个是两个完全独立的实例，前者从未注册`JavaTimeModule`。商品详情接口在写Redis缓存时，缓存的Map里带了`Product`实体的`createdAt`字段（`LocalDateTime`类型），序列化直接失败。

**服务器操作**：代码层修复（`objectMapper.findAndRegisterModules()`），同样要走完整的拉代码→重新构建→导入→重启流程（同问题11）。

### 14. APISIX网关Service改成LoadBalancer后想用80/443，发现要单独放行安全组端口

**现象**：APISIX默认用NodePort暴露（30000-32767区间），改成`service.type=LoadBalancer`后K3s的ServiceLB会把80/443也绑定到节点网卡上，但腾讯云安全组默认只放行了22/80/443（如果中途加过自定义NodePort规则，也要确认80/443本身没被遗漏）。

**服务器操作**：腾讯云控制台 → 安全组 → 入站规则，确认有一条 `来源0.0.0.0/0，协议端口TCP:80` 和 `TCP:443`；如果用NodePort区间（如30681-30683/30720）做过渡测试，对应端口也要单独放行。

---

## 常见问题

**Q: 境内服务器装东西总是连接超时/被重置？**
A: 几乎一定是访问境外资源（Docker Hub、GHCR、quay.io、官方安装脚本）被限制。完整的镜像加速配置和每一步的国内替代方案见[实际部署记录与问题排查](#实际部署记录与问题排查)第1、2条。

**Q: 镜像拉取失败 / ImagePullBackOff？**
A: 先确认是不是上面那条境内网络限制的问题（最常见）；如果镜像本身是自己构建的，检查`image`字段和`imagePullPolicy: IfNotPresent`是否配置正确，私有仓库需要额外配置`imagePullSecrets`。

**Q: HPA 显示 `<unknown>` 的 CPU 使用率？**
A: 通常是 metrics-server 没装好或还在采集冷启动期（等 1-2 分钟），用 `kubectl top pods -n mall` 验证指标是否正常。

**Q: 注册邮件收不到？**
A: 检查 `backend-secret` 里的 `MAIL_*` 是否填写正确，看后端日志 `kubectl logs -n mall deploy/backend`；多数邮箱服务商要求用「SMTP授权码」而不是邮箱登录密码。

**Q: 后端Pod反复重启 / 启动就崩溃？**
A: 先看`kubectl logs -n mall -l app=backend --tail=100 | grep -i error`。常见原因是Hibernate的schema类型校验报错或Jackson序列化报错，具体案例和修复方式见[实际部署记录与问题排查](#实际部署记录与问题排查)第11、13条。

**Q: 改了代码重新部署，但报错内容完全没变？**
A: 大概率是Docker构建缓存复用了旧的层，或者`git pull`实际没拉到最新commit。先用`git log --oneline -1`确认commit对了，再用`docker build --no-cache`强制重新构建（见第10条）。

**Q: 前端中文显示乱码？**
A: 区分清楚是"页面固定文字"还是"数据库读出来的内容"乱码——前者是Nginx/Spring的charset配置问题，后者是种子数据导入时编码错了（更常见），解决方式见第12条。

**Q: backend连不上MySQL/Redis/MinIO，报Connection refused，但这些Pod本身是健康的？**
A: 检查是不是NetworkPolicy里多了一条Egress类型的策略（哪怕只是想"放行DNS"），或者有调试用的Pod复用了`app=backend`这类标签被Service误收编，具体排查方式见第7、8条。

**Q: APISIX网关装完之后路由完全不生效，访问全是404？**
A: 如果用了`ingress-controller.enabled=true`，先看它的日志是否在反复报SSL/Admin API相关错误（`kubectl logs -n apisix -l app.kubernetes.io/name=apisix-ingress-controller`）；本仓库的方案是直接禁用该组件、手动用Admin API管理路由，见第4、5条和[部署到远程服务器·第十六步](#部署到远程服务器)。

**Q: 没有备案域名，HTTPS/域名访问要怎么弄？**
A: 用cert-manager的`SelfSigned` ClusterIssuer签发证书，配合IANA保留的`.test`域名+本机hosts文件映射，完全不需要公网DNS解析，见第9条。

**Q: 后台上传图片/视频后，前端页面显示不出来？**
A: 检查`backend-secret`里的`MINIO_PUBLIC_URL`是否填的是浏览器能直接访问到的地址（NodePort形式是`http://服务器IP:30682`，域名形式则需要网关有对应的转发路由）。先确认浏览器能不能直接打开返回的图片URL，打不开再排查转发配置。

**Q: 忘记管理员密码怎么办？**
A: 进 MySQL 用 BCrypt 重新生成一个密码哈希更新 `t_user` 表（后台暂未提供改密/找回密码功能）。
