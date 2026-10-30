-- =====================================================================
-- WMS 库区主数据
-- 版本: V6
-- 说明:
--   1. 库区唯一口径为“仓库 + 编码”。
--   2. 从现有未删除库位按仓库和库区编码去重补建库区。
--   3. 库区名称优先使用库位备注，无法推导时使用库区编码。
--   4. 使用部分唯一索引兼容历史软删除数据。
-- =====================================================================

CREATE TABLE IF NOT EXISTS wms_zone (
    id            BIGSERIAL    PRIMARY KEY,
    warehouse_id  BIGINT       NOT NULL,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    status        SMALLINT     NOT NULL DEFAULT 1,
    remark        VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES wms_warehouse (id)
);

COMMENT ON TABLE wms_zone IS '库区';
COMMENT ON COLUMN wms_zone.warehouse_id IS '所属仓库';
COMMENT ON COLUMN wms_zone.code IS '库区编码';
COMMENT ON COLUMN wms_zone.name IS '库区名称';
COMMENT ON COLUMN wms_zone.status IS '状态: 1=启用 0=停用';
COMMENT ON COLUMN wms_zone.deleted IS '软删除: 0=正常 1=已删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_zone_warehouse_code
    ON wms_zone (warehouse_id, code)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_zone_warehouse
    ON wms_zone (warehouse_id, deleted);

INSERT INTO wms_zone (warehouse_id, code, name, status, remark, deleted)
SELECT source.warehouse_id,
       source.zone,
       COALESCE(NULLIF(source.remark, ''), source.zone),
       1,
       '由库位历史数据补建',
       0
FROM (
    SELECT l.warehouse_id,
           l.zone,
           MAX(NULLIF(l.remark, '')) AS remark
    FROM wms_location l
    WHERE l.deleted = 0
      AND NULLIF(TRIM(l.zone), '') IS NOT NULL
    GROUP BY l.warehouse_id, l.zone
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM wms_zone z
    WHERE z.warehouse_id = source.warehouse_id
      AND z.code = source.zone
      AND z.deleted = 0
);
