# 项目上下文备忘（写给未来打开这个仓库的Claude Code会话）

这是一份记录"做到哪一步了、为什么这么做"的交接文档，目的是让你（不管是在哪台电脑、哪个新对话里打开这个仓库）能快速接上之前的工作进度，不用重新踩一遍坑。

## 项目是什么

一套云原生电商系统：React前端 + Spring Boot后端 + MySQL/Redis/MinIO + APISIX网关，部署在自建K3s集群上。完整说明见 [README.md](README.md)（功能架构、系统架构、技术栈、完整部署步骤、实际部署问题排查记录）。

## 当前真实部署状态（截至最近一次会话）

- **服务器**：腾讯云CVM，2核4GB，Ubuntu 22.04，公网IP `118.24.43.187`（如果继续在这台服务器上操作，先确认IP有没有变；如果是新服务器，按README从头走一遍）
- **K3s单节点集群**，APISIX网关的Service类型是`LoadBalancer`，K3s自带ServiceLB把80/443直接绑定到节点网卡，访问不用带端口号
- **域名**：`mall.test`（IANA保留的`.test`测试域，只在本机hosts文件里映射，没有走公网DNS——因为服务器在境内且域名未完成ICP备案，公网域名解析到大陆IP会被云厂商拦截）
- **TLS证书**：cert-manager的`SelfSigned`类型`ClusterIssuer`自签（不是Let's Encrypt），浏览器会提示"不受信任"，点继续访问即可，传输仍是加密的
- **默认管理员账号**：`admin` / `admin123`（数据库种子数据自带，生产环境要记得改密，但后台目前没有改密功能，得直接改数据库）

## 几个不要随意"修正"的非标准设计（都是踩坑后刻意这么做的）

1. **APISIX的`ingress-controller`是禁用状态**（`--set ingress-controller.enabled=false`）。[k8s/gateway.yaml](k8s/gateway.yaml) 里的`ApisixRoute`/`ApisixTls` CRD**实际没有生效**，路由是直接调APISIX的Admin API手动创建的（命令见README"第十六步"）。原因：新版ingress-controller的`adc-server`子容器要拉`ghcr.io/api7/adc`镶像，境内拉不动；换旧版本又跟新版APISIX的Admin API在SSL资源同步上不兼容。**如果以后想恢复声明式CRD管理路由，需要先解决这个版本兼容性问题**，不要直接把`ingress-controller.enabled`改回`true`就指望它能用。

2. **`k8s/networkpolicy.yaml`绝对不要加任何`policyTypes: Egress`的策略**，哪怕只是想"顺手放行DNS"。这个仓库之前真的因为这个把自己坑过——一条本意放行DNS的Egress策略，把所有Pod的出站流量都从"默认放行"变成"默认拒绝"，backend直接连不上MySQL。Ingress限制不影响Pod自身出站，DNS解析本来就不需要额外策略。

3. **MySQL导入种子数据必须用`mysql --default-character-set=utf8mb4`**，不能省略这个参数，否则中文会在写入时就编码错误，变成存进数据库里的永久乱码（不是前端/Nginx的charset问题，charset配置救不了已经写错的数据）。

4. **后端的`RedisConfig`里有一个独立`new`出来的`ObjectMapper`**，跟Spring Boot给HTTP响应自动配置的那个不是同一个实例。如果以后改`RedisConfig`，别忘了保留`objectMapper.findAndRegisterModules()`这一行，否则任何带`LocalDateTime`字段的对象写Redis缓存都会序列化失败（商品详情接口500的历史教训）。

5. **境内服务器装任何东西前先确认走没走镜像加速**。`/etc/rancher/k3s/registries.yaml`（K3s/containerd用）和`/etc/docker/daemon.json`（独立Docker daemon用）都配了`docker.m.daocloud.io`/`quay.m.daocloud.io`/`ghcr.m.daocloud.io`镜像加速，这两个文件不在git里（服务器本地配置），如果换了新服务器要重新配一遍。

## Secret管理

所有密钥（数据库密码、JWT密钥、MinIO密钥、邮箱SMTP授权码）都是直接用`kubectl create secret`命令式创建的，**没有写进任何git文件**，仓库里只有[k8s/secret.yaml.template](k8s/secret.yaml.template)这个占位模板。如果需要在新会话里继续操作这台服务器，密钥本身需要SSH登录服务器用`kubectl get secret ... -o jsonpath`重新查，这里不会也不应该记录真实密钥值。

## 还没做完 / 已知的技术债

- 没接真实支付网关（订单详情页的"立即支付"是模拟支付，直接把订单标记为已支付）
- 后台没有改密/找回密码功能
- ingress-controller的版本兼容性问题没有根本解决，路由维护是手动调Admin API，不是declarative的CRD方式（见上面第1条）
- 域名未完成ICP备案，所以只能用`.test`+hosts映射这种过渡方案

## 仓库 / Git

- 远程仓库：`https://github.com/Basarakingaaa/coco-`（GitHub账号最近从`sakuraandkiki`改名过来，如果发现远程地址提示找不到/跳转，确认一下账号名有没有再变过）
- 服务器上的`origin` remote地址需要跟这里保持一致，改账号名之后要在服务器上也跑一遍 `git remote set-url origin https://github.com/Basarakingaaa/coco-.git`
- `report/`目录（答辩用的report.docx + 答辩稿.md，以及生成脚本gen_report.js/gen_code_images.py）**故意没有提交到这个仓库**（已加入.gitignore），只存在于做这份报告时用的那台本机上，如果在别的电脑上找不到这个目录是正常的，不用诧异。

## 完整操作记录

每一步具体命令、每一个踩过的坑和对应的修复方式，全部按时间顺序写在了 [README.md](README.md) 的"实际部署记录与问题排查"章节，遇到类似问题先去那里查，不要凭空猜测重新排查一遍。
