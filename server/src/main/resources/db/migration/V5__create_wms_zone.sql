-- =====================================================================
-- WMS 仓储管理系统 — 库区
-- 版本: V5
-- 说明: 库区属于仓库，库位通过 zone_id 归属于库区。
-- =====================================================================

CREATE TABLE wms_zone (
    id            BIGSERIAL    PRIMARY KEY,
    warehouse_id  BIGINT       NOT NULL,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
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

ALTER TABLE wms_location
    ADD COLUMN zone_id BIGINT,
    ADD CONSTRAINT fk_location_zone FOREIGN KEY (zone_id)
        REFERENCES wms_zone (id);

CREATE INDEX idx_location_zone_id ON wms_location (zone_id, deleted);

COMMENT ON COLUMN wms_location.zone_id IS '所属库区';
