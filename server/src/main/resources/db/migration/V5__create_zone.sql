-- =====================================================================
-- WMS 仓储管理系统 — 库区
-- 版本: V5
-- 说明: 库区是仓库下的分区，库位通过 warehouse_id + zone 编码归属库区。
-- =====================================================================

CREATE TABLE wms_zone (
    id           BIGSERIAL    PRIMARY KEY,
    warehouse_id BIGINT       NOT NULL,
    code         VARCHAR(32)  NOT NULL,
    name         VARCHAR(128) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_zone_warehouse_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_zone             IS '库区';
COMMENT ON COLUMN wms_zone.warehouse_id IS '所属仓库';
COMMENT ON COLUMN wms_zone.code         IS '库区编码';
COMMENT ON COLUMN wms_zone.name         IS '库区名称';
COMMENT ON COLUMN wms_zone.deleted      IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_zone_warehouse ON wms_zone (warehouse_id, deleted);
CREATE INDEX idx_zone_deleted   ON wms_zone (deleted);
CREATE INDEX idx_zone_name      ON wms_zone (name);
