CREATE TABLE IF NOT EXISTS wms_zone (
    id          BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    status      INTEGER NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_wms_zone_warehouse_code UNIQUE (warehouse_id, code),
    CONSTRAINT fk_wms_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse(id)
);

CREATE INDEX IF NOT EXISTS idx_wms_zone_warehouse_id
    ON wms_zone (warehouse_id);

CREATE INDEX IF NOT EXISTS idx_wms_zone_status_deleted
    ON wms_zone (status, deleted);
