-- =====================================================================
-- WMS 仓储管理系统 — 基础主数据 schema
-- 版本: V1
-- 说明: 库存在 PostgreSQL 上由 Flyway 管理，本文件是唯一事实来源。
--       约定: 所有业务表带 created_at / updated_at / deleted 软删除标记。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 仓库 warehouse
-- ---------------------------------------------------------------------
CREATE TABLE wms_warehouse (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    address       VARCHAR(255),
    contact_name  VARCHAR(64),
    contact_phone VARCHAR(32),
    status        SMALLINT     NOT NULL DEFAULT 1,
    remark        VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_warehouse_code UNIQUE (code)
);
COMMENT ON TABLE  wms_warehouse                IS '仓库';
COMMENT ON COLUMN wms_warehouse.status         IS '状态: 1=启用 0=停用';
COMMENT ON COLUMN wms_warehouse.deleted        IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_warehouse_deleted ON wms_warehouse (deleted);
CREATE INDEX idx_warehouse_name   ON wms_warehouse (name);

-- ---------------------------------------------------------------------
-- 2. 库位 location
-- ---------------------------------------------------------------------
CREATE TABLE wms_location (
    id              BIGSERIAL    PRIMARY KEY,
    warehouse_id    BIGINT       NOT NULL,
    code            VARCHAR(32)  NOT NULL,
    zone            VARCHAR(32)  NOT NULL,
    location_type   SMALLINT     NOT NULL DEFAULT 1,
    capacity        NUMERIC(18,4),
    allow_negative  SMALLINT     NOT NULL DEFAULT 0,
    status          SMALLINT     NOT NULL DEFAULT 1,
    remark          VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_location_wh_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_location_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_location                   IS '库位';
COMMENT ON COLUMN wms_location.zone              IS '库区编码，如 A/B/C';
COMMENT ON COLUMN wms_location.location_type     IS '库位类型: 1=存储位 2=收货暂存位 3=发货暂存位 4=不良品位 5=退货位';
COMMENT ON COLUMN wms_location.capacity          IS '容量，NULL 表示不限制';
COMMENT ON COLUMN wms_location.allow_negative    IS '是否允许负库存: 1=允许 0=不允许';
COMMENT ON COLUMN wms_location.status            IS '状态: 1=启用 0=停用';
CREATE INDEX idx_location_warehouse ON wms_location (warehouse_id, deleted);
CREATE INDEX idx_location_zone       ON wms_location (zone);

-- ---------------------------------------------------------------------
-- 3. 商品 SKU
-- ---------------------------------------------------------------------
CREATE TABLE wms_sku (
    id                BIGSERIAL    PRIMARY KEY,
    code              VARCHAR(64)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    category          VARCHAR(64),
    spec              VARCHAR(64),
    unit              VARCHAR(16),
    barcode           VARCHAR(64),
    batch_managed     SMALLINT     NOT NULL DEFAULT 0,
    serial_managed    SMALLINT     NOT NULL DEFAULT 0,
    shelf_life_days   INTEGER,
    weight            NUMERIC(18,4),
    volume            NUMERIC(18,4),
    length            NUMERIC(18,4),
    width             NUMERIC(18,4),
    height            NUMERIC(18,4),
    status            SMALLINT     NOT NULL DEFAULT 1,
    remark            VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_sku_code UNIQUE (code)
);
COMMENT ON TABLE  wms_sku                   IS '商品 SKU';
COMMENT ON COLUMN wms_sku.batch_managed      IS '是否批次管理: 1=是 0=否';
COMMENT ON COLUMN wms_sku.serial_managed     IS '是否序列号管理: 1=是 0=否';
COMMENT ON COLUMN wms_sku.shelf_life_days    IS '保质期天数，NULL 表示无保质期';
COMMENT ON COLUMN wms_sku.weight             IS '重量，单位与 unit 对应';
COMMENT ON COLUMN wms_sku.volume             IS '体积';
COMMENT ON COLUMN wms_sku.status             IS '状态: 1=启用 0=停用';
CREATE INDEX idx_sku_deleted  ON wms_sku (deleted);
CREATE INDEX idx_sku_name     ON wms_sku (name);
CREATE INDEX idx_sku_category ON wms_sku (category);
CREATE INDEX idx_sku_barcode  ON wms_sku (barcode);

-- ---------------------------------------------------------------------
-- 4. 客户 customer
-- ---------------------------------------------------------------------
CREATE TABLE wms_customer (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    contact_name  VARCHAR(64),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    address       VARCHAR(255),
    status        SMALLINT     NOT NULL DEFAULT 1,
    remark        VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_code UNIQUE (code)
);
COMMENT ON TABLE  wms_customer         IS '客户';
COMMENT ON COLUMN wms_customer.status  IS '状态: 1=启用 0=停用';
CREATE INDEX idx_customer_deleted ON wms_customer (deleted);
CREATE INDEX idx_customer_name    ON wms_customer (name);
