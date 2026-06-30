-- ============================================
-- Mall 本地数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall;

-- ------------------------------------------------
-- 用户表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(128) NOT NULL,
    phone        VARCHAR(20),
    email        VARCHAR(128),
    avatar       VARCHAR(255),
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status       TINYINT      NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品分类表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_product_category (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    sort_order   INT          NOT NULL DEFAULT 0,
    icon         VARCHAR(255),
    status       TINYINT      NOT NULL DEFAULT 1,
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品表
-- ------------------------------------------------
-- status：商品是否上架（1=上架 0=下架），与资料（图片/视频）是否存在完全无关——
-- 商品可以在没有任何资料的情况下创建/上架，资料的增删也不会改变这里的上架状态。
CREATE TABLE IF NOT EXISTS t_product (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT         NOT NULL,
    name         VARCHAR(128)   NOT NULL,
    subtitle     VARCHAR(255),
    price        DECIMAL(10,2)  NOT NULL,
    stock        INT            NOT NULL DEFAULT 0,
    sales        INT            NOT NULL DEFAULT 0,
    status       TINYINT        NOT NULL DEFAULT 1,
    created_at   DATETIME       NOT NULL,
    updated_at   DATETIME       NOT NULL,
    KEY idx_category_id (category_id),
    KEY idx_name (name)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品资料表：图片/视频，与商品本体（t_product）解耦。
-- 一个商品可以有 0~N 条资料；某条资料的增删不影响商品的上架状态，反之亦然。
-- media_type = IMAGE（封面图取 sort_order 最小的一条）或 VIDEO。
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_product_media (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    media_type   VARCHAR(10)  NOT NULL,
    url          VARCHAR(500) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    KEY idx_product_id (product_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品子信息1：图文详情
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_product_info1 (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    image_url    VARCHAR(255),
    detail_html  LONGTEXT,
    sort_order   INT          NOT NULL DEFAULT 0,
    KEY idx_product_id (product_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品子信息2：规格参数
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_product_info2 (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    spec_key     VARCHAR(64)  NOT NULL,
    spec_value   VARCHAR(255) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    KEY idx_product_id (product_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 商品 SKU 表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_product_sku (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT         NOT NULL,
    sku_code     VARCHAR(64)    NOT NULL,
    spec_desc    VARCHAR(255),
    price        DECIMAL(10,2)  NOT NULL,
    stock        INT            NOT NULL DEFAULT 0,
    image        VARCHAR(255),
    status       TINYINT        NOT NULL DEFAULT 1,
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 广告类别表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_ad_category (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    status       TINYINT     NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 广告表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_advertisement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT       NOT NULL,
    title        VARCHAR(128) NOT NULL,
    image_url    VARCHAR(255) NOT NULL,
    link_url     VARCHAR(255),
    sort_order   INT          NOT NULL DEFAULT 0,
    start_time   DATETIME,
    end_time     DATETIME,
    status       TINYINT      NOT NULL DEFAULT 1,
    KEY idx_category_id (category_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 购物车表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_cart_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT   NOT NULL,
    product_id   BIGINT   NOT NULL,
    sku_id       BIGINT,
    quantity     INT      NOT NULL,
    selected     TINYINT  NOT NULL DEFAULT 1,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 订单表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL,
    user_id          BIGINT         NOT NULL,
    total_amount     DECIMAL(10,2)  NOT NULL,
    status           TINYINT        NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已发货 3已完成 4已取消',
    receiver_name    VARCHAR(64)    NOT NULL,
    receiver_phone   VARCHAR(20)    NOT NULL,
    receiver_address VARCHAR(255)   NOT NULL,
    paid_at          DATETIME,
    created_at       DATETIME       NOT NULL,
    updated_at       DATETIME       NOT NULL,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;

-- ------------------------------------------------
-- 订单项表
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order_item (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT         NOT NULL,
    product_id     BIGINT         NOT NULL,
    sku_id         BIGINT,
    product_name   VARCHAR(128)   NOT NULL,
    product_image  VARCHAR(255),
    price          DECIMAL(10,2)  NOT NULL,
    quantity       INT            NOT NULL,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB;

-- ============================================
-- 种子数据
-- ============================================

-- 默认管理员账号：用户名 admin / 密码 admin123
-- 部署到生产环境后请立即登录后台修改密码（目前后台暂未提供改密接口，可直接更新本表 password 字段为新的 BCrypt 哈希）
INSERT INTO t_user (username, password, role, status, created_at, updated_at) VALUES
('admin', '$2b$10$Hd4x.5urcRPngJSju1gLIOmDRWBlR58xz69Y5KoASZINazdPsFfsK', 'ADMIN', 1, NOW(), NOW());

INSERT INTO t_product_category (name, parent_id, sort_order, status) VALUES
('数码电器', 0, 1, 1),
('服装鞋包', 0, 2, 1),
('食品生鲜', 0, 3, 1);

INSERT INTO t_ad_category (name, status) VALUES
('首页轮播', 1),
('分类页横幅', 1);

INSERT INTO t_advertisement (category_id, title, image_url, link_url, sort_order, status) VALUES
(1, '618大促', 'https://picsum.photos/seed/mall-ad1/1200/480', '/products?categoryId=1', 1, 1),
(1, '新品上市', 'https://picsum.photos/seed/mall-ad2/1200/480', '/products?categoryId=2', 2, 1);

INSERT INTO t_product (category_id, name, subtitle, price, stock, sales, status, created_at, updated_at) VALUES
(1, '无线蓝牙耳机', '主动降噪 长续航', 299.00, 500, 120, 1, NOW(), NOW()),
(1, '智能手表', '健康监测 七天续航', 899.00, 200, 60, 1, NOW(), NOW()),
(1, '便携蓝牙音箱', '防水设计 立体声', 159.00, 350, 90, 1, NOW(), NOW()),
(2, '休闲运动鞋', '透气舒适 百搭款', 199.00, 300, 80, 1, NOW(), NOW()),
(2, '羽绒保暖外套', '轻量蓄热 防风防水', 459.00, 150, 45, 1, NOW(), NOW()),
(3, '新鲜草莓 1kg', '当季采摘 顺丰冷链', 39.90, 800, 320, 1, NOW(), NOW()),
-- 故意不给这个商品配任何资料，演示“已上架但暂无资料”的解耦状态
(3, '云南小粒咖啡豆', '中度烘焙 现磨现发', 49.90, 200, 0, 1, NOW(), NOW());

-- 商品资料：图片/视频与商品记录完全独立维护，sort_order 决定同类型资料的展示顺序
INSERT INTO t_product_media (product_id, media_type, url, sort_order, created_at) VALUES
(1, 'IMAGE', 'https://picsum.photos/seed/mall-earbud/600/600', 0, NOW()),
(1, 'VIDEO', 'https://www.w3schools.com/html/mov_bbb.mp4', 1, NOW()),
(2, 'IMAGE', 'https://picsum.photos/seed/mall-watch/600/600', 0, NOW()),
(3, 'IMAGE', 'https://picsum.photos/seed/mall-speaker/600/600', 0, NOW()),
(4, 'IMAGE', 'https://picsum.photos/seed/mall-shoes/600/600', 0, NOW()),
(5, 'IMAGE', 'https://picsum.photos/seed/mall-jacket/600/600', 0, NOW()),
(5, 'VIDEO', 'https://www.w3schools.com/html/mov_bbb.mp4', 1, NOW()),
(6, 'IMAGE', 'https://picsum.photos/seed/mall-strawberry/600/600', 0, NOW());
-- 商品 7（云南小粒咖啡豆）没有对应的 t_product_media 记录 —— 这就是“没有资料”状态

INSERT INTO t_product_info1 (product_id, image_url, detail_html, sort_order) VALUES
(1, 'https://picsum.photos/seed/mall-earbud-detail1/900/900', '<p>主动降噪，沉浸式聆听体验。</p>', 1),
(2, 'https://picsum.photos/seed/mall-watch-detail1/900/900', '<p>24小时健康监测，运动数据实时同步。</p>', 1),
(4, 'https://picsum.photos/seed/mall-shoes-detail1/900/900', '<p>轻量网面材质，长时间穿着不闷脚。</p>', 1),
(6, 'https://picsum.photos/seed/mall-strawberry-detail1/900/900', '<p>当季现摘，48小时内冷链直达。</p>', 1);

INSERT INTO t_product_info2 (product_id, spec_key, spec_value, sort_order) VALUES
(1, '品牌', 'MallTech', 1),
(1, '蓝牙版本', '5.3', 2),
(2, '品牌', 'MallTech', 1),
(2, '续航', '7天', 2),
(3, '品牌', 'MallTech', 1),
(3, '防水等级', 'IPX7', 2),
(4, '品牌', 'MallShoes', 1),
(4, '材质', '网面', 2),
(5, '品牌', 'MallWear', 1),
(5, '填充物', '90%白鹅绒', 2),
(6, '产地', '云南', 1),
(6, '规格', '1kg/盒', 2);

INSERT INTO t_product_sku (product_id, sku_code, spec_desc, price, stock, image, status) VALUES
(1, 'EARBUD-BLACK', '黑色', 299.00, 250, 'https://picsum.photos/seed/mall-earbud-black/600/600', 1),
(1, 'EARBUD-WHITE', '白色', 299.00, 250, 'https://picsum.photos/seed/mall-earbud-white/600/600', 1),
(4, 'SHOE-40', '40码', 199.00, 100, NULL, 1),
(4, 'SHOE-41', '41码', 199.00, 100, NULL, 1),
(5, 'JACKET-M', 'M码 藏青', 459.00, 60, 'https://picsum.photos/seed/mall-jacket-m/600/600', 1),
(5, 'JACKET-L', 'L码 藏青', 459.00, 60, 'https://picsum.photos/seed/mall-jacket-l/600/600', 1);
