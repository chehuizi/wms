-- =====================================================================
-- WMS 仓储管理系统 — 库区模块
-- =====================================================================
CREATE TABLE wms_zone (
    id            BIGSERIAL    PRIMARY KEY,
    warehouse_id  BIGINT       NOT NULL,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    status        SMALLINT     NOT NULL DEFAULT 1,
    remark        VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_zone_wh_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_zone                IS '库区';
COMMENT ON COLUMN wms_zone.warehouse_id    IS '所属仓库';
COMMENT ON COLUMN wms_zone.code            IS '库区编码';
COMMENT ON COLUMN wms_zone.name            IS '库区名称';
COMMENT ON COLUMN wms_zone.status          IS '状态: 1=启用 0=停用';
COMMENT ON COLUMN wms_zone.deleted         IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_zone_warehouse ON wms_zone (warehouse_id, deleted);
CREATE INDEX idx_zone_name      ON wms_zone (name);
