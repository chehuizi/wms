-- =====================================================================
-- WMS 仓储管理系统 — 库区基础主数据
-- 版本: V5
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
COMMENT ON TABLE  wms_zone                IS '库区';
COMMENT ON COLUMN wms_zone.warehouse_id  IS '所属仓库ID';
COMMENT ON COLUMN wms_zone.code          IS '库区编码';
COMMENT ON COLUMN wms_zone.deleted       IS '软删除: 0=正常 1=已删除';
CREATE INDEX idx_zone_warehouse ON wms_zone (warehouse_id, deleted);
CREATE INDEX idx_zone_code      ON wms_zone (code);

INSERT INTO wms_zone (warehouse_id, code, name)
SELECT w.id, v.code, v.name
FROM wms_warehouse w
JOIN (VALUES
  ('WH01', 'A',   'A区'),
  ('WH01', 'B',   'B区'),
  ('WH01', 'RCV', '收货暂存区'),
  ('WH01', 'SHP', '发货暂存区'),
  ('WH01', 'DEF', '不良品区'),
  ('WH02', 'A',   'A区')
) AS v(warehouse_code, code, name)
  ON w.code = v.warehouse_code
WHERE w.deleted = 0;
