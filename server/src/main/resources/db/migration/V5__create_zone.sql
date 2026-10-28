-- =====================================================================
-- WMS 库区模块
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
COMMENT ON COLUMN wms_zone.deleted     IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_zone_warehouse ON wms_zone (warehouse_id, deleted);
CREATE INDEX idx_zone_name      ON wms_zone (name);
