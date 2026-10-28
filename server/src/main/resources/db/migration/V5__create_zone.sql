-- =====================================================================
-- WMS 仓储管理系统 — 库区
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
COMMENT ON TABLE  wms_zone               IS '库区，隶属于仓库';
COMMENT ON COLUMN wms_zone.warehouse_id IS '所属仓库';
COMMENT ON COLUMN wms_zone.code         IS '库区编码';
COMMENT ON COLUMN wms_zone.name         IS '库区名称';
COMMENT ON COLUMN wms_zone.deleted      IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_zone_warehouse ON wms_zone (warehouse_id, deleted);
CREATE INDEX idx_zone_name     ON wms_zone (name);

ALTER TABLE wms_location
    ADD COLUMN zone_id BIGINT;

UPDATE wms_location l
SET zone_id = z.id
FROM wms_zone z
WHERE z.deleted = 0
  AND l.warehouse_id = z.warehouse_id
  AND l.zone = z.code;

ALTER TABLE wms_location
    ADD CONSTRAINT fk_location_zone FOREIGN KEY (zone_id)
        REFERENCES wms_zone (id);

CREATE INDEX idx_location_zone_id ON wms_location (zone_id);
