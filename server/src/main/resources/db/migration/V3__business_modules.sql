-- =====================================================================
-- WMS 仓储管理系统 — 核心业务模块
--   库存台账 / 库存流水 / 序列号台账
--   入库（到货单 → 收货 → 上架）
--   出库（订单 → 分配锁定 → 拣货 → 发货）
--   盘点（盘点单 → 实盘 → 差异审批 → 调账）
-- 版本: V3
-- 说明: 本文件依赖 V1 的 wms_warehouse / wms_location / wms_sku。
--       要求 PostgreSQL >= 15（本文件使用 NULLS NOT DISTINCT）。
-- =====================================================================

-- =====================================================================
-- 1. 库存台账
--    唯一键: 库位 + SKU + 批次 + 序列号（NULL 视为同一个空值）
--    可用量 available_qty = on_hand_qty - locked_qty，不落库、实时算
-- =====================================================================
CREATE TABLE wms_inventory (
    id             BIGSERIAL      PRIMARY KEY,
    warehouse_id   BIGINT         NOT NULL,
    location_id    BIGINT         NOT NULL,
    sku_id         BIGINT         NOT NULL,
    batch_no       VARCHAR(64),
    serial_no      VARCHAR(64),
    produced_date  DATE,
    expire_date    DATE,
    on_hand_qty    NUMERIC(18,4)  NOT NULL DEFAULT 0,
    locked_qty     NUMERIC(18,4)  NOT NULL DEFAULT 0,
    status         SMALLINT       NOT NULL DEFAULT 1,
    remark         VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted        SMALLINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id),
    CONSTRAINT fk_inventory_location  FOREIGN KEY (location_id)  REFERENCES wms_location (id),
    CONSTRAINT fk_inventory_sku       FOREIGN KEY (sku_id)       REFERENCES wms_sku (id),
    CONSTRAINT ck_inventory_on_hand   CHECK (on_hand_qty >= 0),
    CONSTRAINT ck_inventory_locked    CHECK (locked_qty  >= 0),
    CONSTRAINT ck_inventory_not_over_locked CHECK (locked_qty <= on_hand_qty),
    CONSTRAINT uk_inventory_slot UNIQUE NULLS NOT DISTINCT (location_id, sku_id, batch_no, serial_no)
);
COMMENT ON TABLE  wms_inventory                      IS '库存台账';
COMMENT ON COLUMN wms_inventory.on_hand_qty           IS '在库数量';
COMMENT ON COLUMN wms_inventory.locked_qty            IS '锁定数量（已分配未出库）';
COMMENT ON COLUMN wms_inventory.status                IS '状态: 1=正常 0=冻结';
COMMENT ON CONSTRAINT ck_inventory_not_over_locked ON wms_inventory IS '锁定量不得超过在库量';

CREATE INDEX idx_inventory_sku      ON wms_inventory (sku_id, deleted);
CREATE INDEX idx_inventory_wh       ON wms_inventory (warehouse_id, deleted);
CREATE INDEX idx_inventory_location ON wms_inventory (location_id);
CREATE INDEX idx_inventory_batch    ON wms_inventory (batch_no);
CREATE INDEX idx_inventory_serial   ON wms_inventory (serial_no);

-- =====================================================================
-- 2. 库存流水（只增不改，账实一致的审计依据）
--    每一次库存/锁定变化都必须留一条，同事务写入
-- =====================================================================
CREATE TABLE wms_stock_movement (
    id             BIGSERIAL      PRIMARY KEY,
    movement_no    VARCHAR(32)    NOT NULL,
    movement_type  SMALLINT       NOT NULL,
    biz_type       VARCHAR(32),
    biz_no         VARCHAR(32),
    warehouse_id   BIGINT         NOT NULL,
    location_id    BIGINT         NOT NULL,
    sku_id         BIGINT         NOT NULL,
    batch_no       VARCHAR(64),
    serial_no      VARCHAR(64),
    qty_change     NUMERIC(18,4)  NOT NULL DEFAULT 0,
    locked_change  NUMERIC(18,4)  NOT NULL DEFAULT 0,
    before_qty     NUMERIC(18,4),
    after_qty      NUMERIC(18,4),
    operator       VARCHAR(64),
    remark         VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_movement_no UNIQUE (movement_no),
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id),
    CONSTRAINT fk_movement_sku      FOREIGN KEY (sku_id)      REFERENCES wms_sku (id)
);
COMMENT ON TABLE  wms_stock_movement               IS '库存流水，只增不改';
COMMENT ON COLUMN wms_stock_movement.movement_type  IS '类型: 1=入库收货 2=出库发货 3=盘盈 4=盘亏 5=移库出 6=移库入 7=锁定 8=解锁 9=手工调整';
COMMENT ON COLUMN wms_stock_movement.qty_change     IS '在库量变化，可正可负';
COMMENT ON COLUMN wms_stock_movement.locked_change  IS '锁定量变化，可正可负';
COMMENT ON COLUMN wms_stock_movement.before_qty     IS '变动前在库量快照';
COMMENT ON COLUMN wms_stock_movement.after_qty      IS '变动后在库量快照';

CREATE INDEX idx_movement_sku      ON wms_stock_movement (sku_id, created_at DESC);
CREATE INDEX idx_movement_biz      ON wms_stock_movement (biz_type, biz_no);
CREATE INDEX idx_movement_created  ON wms_stock_movement (created_at DESC);
CREATE INDEX idx_movement_type     ON wms_stock_movement (movement_type);

-- =====================================================================
-- 3. 序列号台账（SKU 开启 serial_managed 时使用）
-- =====================================================================
CREATE TABLE wms_serial (
    id            BIGSERIAL      PRIMARY KEY,
    serial_no     VARCHAR(64)    NOT NULL,
    sku_id        BIGINT         NOT NULL,
    warehouse_id  BIGINT,
    location_id   BIGINT,
    status        SMALLINT       NOT NULL DEFAULT 1,
    in_bound_at   TIMESTAMPTZ,
    out_bound_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted       SMALLINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_serial_no UNIQUE (serial_no),
    CONSTRAINT fk_serial_sku FOREIGN KEY (sku_id) REFERENCES wms_sku (id)
);
COMMENT ON TABLE  wms_serial        IS '序列号台账';
COMMENT ON COLUMN wms_serial.status IS '状态: 1=在库 2=已锁定 3=已出库 4=已退货';

CREATE INDEX idx_serial_sku  ON wms_serial (sku_id, status);
CREATE INDEX idx_serial_wh   ON wms_serial (warehouse_id);

-- =====================================================================
-- 4. 入库 — 到货单（ASN）
-- =====================================================================
CREATE TABLE wms_inbound_order (
    id             BIGSERIAL      PRIMARY KEY,
    order_no       VARCHAR(32)    NOT NULL,
    warehouse_id   BIGINT         NOT NULL,
    supplier_code  VARCHAR(32),
    supplier_name  VARCHAR(128),
    status         SMALLINT       NOT NULL DEFAULT 1,
    expected_at    TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    remark         VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted        SMALLINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_inbound_order_no UNIQUE (order_no),
    CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_inbound_order            IS '入库到货单 ASN';
COMMENT ON COLUMN wms_inbound_order.status     IS '状态: 1=草稿 2=待收货 3=部分收货 4=已完成 5=已取消';
COMMENT ON COLUMN wms_inbound_order.finished_at IS '全部收货完成时间';

CREATE INDEX idx_inbound_wh      ON wms_inbound_order (warehouse_id, deleted);
CREATE INDEX idx_inbound_status  ON wms_inbound_order (status);

CREATE TABLE wms_inbound_item (
    id             BIGSERIAL      PRIMARY KEY,
    inbound_id     BIGINT         NOT NULL,
    sku_id         BIGINT         NOT NULL,
    expected_qty   NUMERIC(18,4)  NOT NULL,
    received_qty   NUMERIC(18,4)  NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_inbound_item_order FOREIGN KEY (inbound_id) REFERENCES wms_inbound_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_inbound_item_sku   FOREIGN KEY (sku_id)     REFERENCES wms_sku (id),
    CONSTRAINT ck_inbound_item_expected CHECK (expected_qty > 0),
    CONSTRAINT ck_inbound_item_received CHECK (received_qty >= 0)
);
COMMENT ON TABLE  wms_inbound_item IS '到货单明细';
CREATE INDEX idx_inbound_item_order ON wms_inbound_item (inbound_id);
CREATE INDEX idx_inbound_item_sku   ON wms_inbound_item (sku_id);

-- ---- 收货记录：一张到货单可以分多次收货，每次落到一个收货暂存位 ----
CREATE TABLE wms_inbound_receipt (
    id             BIGSERIAL      PRIMARY KEY,
    receipt_no     VARCHAR(32)    NOT NULL,
    inbound_id     BIGINT         NOT NULL,
    location_id    BIGINT         NOT NULL,
    received_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    operator       VARCHAR(64),
    remark         VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_inbound_receipt_no UNIQUE (receipt_no),
    CONSTRAINT fk_inbound_receipt_order FOREIGN KEY (inbound_id)  REFERENCES wms_inbound_order (id),
    CONSTRAINT fk_inbound_receipt_loc   FOREIGN KEY (location_id) REFERENCES wms_location (id)
);
COMMENT ON TABLE  wms_inbound_receipt IS '入库收货记录';
CREATE INDEX idx_inbound_receipt_order ON wms_inbound_receipt (inbound_id);

CREATE TABLE wms_inbound_receipt_item (
    id                BIGSERIAL      PRIMARY KEY,
    receipt_id        BIGINT         NOT NULL,
    inbound_item_id   BIGINT         NOT NULL,
    sku_id            BIGINT         NOT NULL,
    batch_no          VARCHAR(64),
    serial_no         VARCHAR(64),
    produced_date     DATE,
    expire_date       DATE,
    qty               NUMERIC(18,4)  NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_receipt_item_receipt FOREIGN KEY (receipt_id)      REFERENCES wms_inbound_receipt (id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_item_sku     FOREIGN KEY (sku_id)          REFERENCES wms_sku (id),
    CONSTRAINT ck_receipt_item_qty     CHECK (qty > 0)
);
COMMENT ON TABLE  wms_inbound_receipt_item IS '收货明细';
CREATE INDEX idx_receipt_item_receipt ON wms_inbound_receipt_item (receipt_id);

-- ---- 上架任务：收货暂存位 → 存储位，触发移库流水 ----
CREATE TABLE wms_putaway_task (
    id                BIGSERIAL      PRIMARY KEY,
    task_no           VARCHAR(32)    NOT NULL,
    receipt_item_id   BIGINT         NOT NULL,
    inbound_id        BIGINT         NOT NULL,
    sku_id            BIGINT         NOT NULL,
    from_location_id  BIGINT         NOT NULL,
    -- 目标库位可空：上架任务在「收货时」自动生成，此时操作员还没指定目标位，
    -- to_location_id 在「执行上架」时才写入。未上架前为 NULL 是正常状态。
    to_location_id    BIGINT,
    batch_no          VARCHAR(64),
    serial_no         VARCHAR(64),
    qty               NUMERIC(18,4)  NOT NULL,
    status            SMALLINT       NOT NULL DEFAULT 1,
    operator          VARCHAR(64),
    executed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_putaway_task_no UNIQUE (task_no),
    CONSTRAINT fk_putaway_receipt_item FOREIGN KEY (receipt_item_id) REFERENCES wms_inbound_receipt_item (id),
    CONSTRAINT fk_putaway_inbound      FOREIGN KEY (inbound_id)      REFERENCES wms_inbound_order (id),
    CONSTRAINT fk_putaway_sku          FOREIGN KEY (sku_id)          REFERENCES wms_sku (id),
    -- 上架完成后目标位必须已填，且不能与来源位相同
    CONSTRAINT ck_putaway_done CHECK (
        status <> 2 OR (to_location_id IS NOT NULL AND to_location_id <> from_location_id)
    ),
    CONSTRAINT ck_putaway_qty CHECK (qty > 0)
);
COMMENT ON TABLE  wms_putaway_task            IS '上架任务，收货时自动生成，to_location_id 在执行上架时才写入';
COMMENT ON COLUMN wms_putaway_task.to_location_id IS '目标存储位，未上架时为 NULL';
COMMENT ON COLUMN wms_putaway_task.status     IS '状态: 1=待上架 2=已完成 3=已取消';

CREATE INDEX idx_putaway_inbound ON wms_putaway_task (inbound_id, status);
CREATE INDEX idx_putaway_sku     ON wms_putaway_task (sku_id);

-- =====================================================================
-- 5. 出库 — 销售订单
-- =====================================================================
CREATE TABLE wms_outbound_order (
    id                BIGSERIAL      PRIMARY KEY,
    order_no          VARCHAR(32)    NOT NULL,
    warehouse_id      BIGINT         NOT NULL,
    customer_id       BIGINT,
    status            SMALLINT       NOT NULL DEFAULT 1,
    priority          SMALLINT       NOT NULL DEFAULT 0,
    expected_ship_at  TIMESTAMPTZ,
    shipped_at        TIMESTAMPTZ,
    remark            VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted           SMALLINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_outbound_order_no UNIQUE (order_no),
    CONSTRAINT fk_outbound_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id),
    CONSTRAINT fk_outbound_customer  FOREIGN KEY (customer_id)  REFERENCES wms_customer (id)
);
COMMENT ON TABLE  wms_outbound_order           IS '出库订单';
COMMENT ON COLUMN wms_outbound_order.status    IS '状态: 1=草稿 2=待分配 3=已分配 4=拣货中 5=待发货 6=已发货 7=已取消';
COMMENT ON COLUMN wms_outbound_order.priority  IS '优先级，数值越大越优先';

CREATE INDEX idx_outbound_wh     ON wms_outbound_order (warehouse_id, deleted);
CREATE INDEX idx_outbound_status ON wms_outbound_order (status, priority DESC);

CREATE TABLE wms_outbound_item (
    id            BIGSERIAL      PRIMARY KEY,
    outbound_id   BIGINT         NOT NULL,
    sku_id        BIGINT         NOT NULL,
    ordered_qty   NUMERIC(18,4)  NOT NULL,
    allocated_qty NUMERIC(18,4)  NOT NULL DEFAULT 0,
    picked_qty    NUMERIC(18,4)  NOT NULL DEFAULT 0,
    shipped_qty   NUMERIC(18,4)  NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_outbound_item_order FOREIGN KEY (outbound_id) REFERENCES wms_outbound_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_item_sku   FOREIGN KEY (sku_id)      REFERENCES wms_sku (id),
    CONSTRAINT ck_outbound_item_ordered  CHECK (ordered_qty > 0),
    CONSTRAINT ck_outbound_item_allocated CHECK (allocated_qty >= 0 AND allocated_qty <= ordered_qty),
    CONSTRAINT ck_outbound_item_picked   CHECK (picked_qty >= 0 AND picked_qty <= allocated_qty),
    CONSTRAINT ck_outbound_item_shipped  CHECK (shipped_qty >= 0 AND shipped_qty <= picked_qty)
);
COMMENT ON TABLE  wms_outbound_item IS '出库订单明细';
CREATE INDEX idx_outbound_item_order ON wms_outbound_item (outbound_id);
CREATE INDEX idx_outbound_item_sku   ON wms_outbound_item (sku_id);

-- ---- 分配记录：订单锁定库存的具体来源行 ----
CREATE TABLE wms_allocation (
    id               BIGSERIAL      PRIMARY KEY,
    outbound_id      BIGINT         NOT NULL,
    outbound_item_id BIGINT         NOT NULL,
    inventory_id     BIGINT         NOT NULL,
    warehouse_id     BIGINT         NOT NULL,
    location_id      BIGINT         NOT NULL,
    sku_id           BIGINT         NOT NULL,
    batch_no         VARCHAR(64),
    allocated_qty    NUMERIC(18,4)  NOT NULL,
    picked_qty       NUMERIC(18,4)  NOT NULL DEFAULT 0,
    shipped_qty      NUMERIC(18,4)  NOT NULL DEFAULT 0,
    status           SMALLINT       NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_allocation_outbound FOREIGN KEY (outbound_id)      REFERENCES wms_outbound_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_item     FOREIGN KEY (outbound_item_id) REFERENCES wms_outbound_item (id),
    CONSTRAINT fk_allocation_inventory FOREIGN KEY (inventory_id)   REFERENCES wms_inventory (id),
    CONSTRAINT ck_allocation_qty CHECK (allocated_qty > 0)
);
COMMENT ON TABLE  wms_allocation         IS '库存分配记录（订单锁库存）';
COMMENT ON COLUMN wms_allocation.status IS '状态: 1=已锁定 2=拣货中 3=已拣货 4=已发货 5=已释放';

CREATE INDEX idx_allocation_outbound ON wms_allocation (outbound_id, status);
CREATE INDEX idx_allocation_inv      ON wms_allocation (inventory_id);

-- ---- 波次：把多张订单合并拣货 ----
CREATE TABLE wms_wave (
    id            BIGSERIAL      PRIMARY KEY,
    wave_no       VARCHAR(32)    NOT NULL,
    warehouse_id  BIGINT         NOT NULL,
    status        SMALLINT       NOT NULL DEFAULT 1,
    order_count   INTEGER        NOT NULL DEFAULT 0,
    released_at   TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    operator      VARCHAR(64),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_wave_no UNIQUE (wave_no),
    CONSTRAINT fk_wave_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_wave         IS '拣货波次';
COMMENT ON COLUMN wms_wave.status  IS '状态: 1=创建中 2=已释放 3=拣货中 4=已完成 5=已关闭';

CREATE INDEX idx_wave_wh ON wms_wave (warehouse_id, status);

CREATE TABLE wms_wave_order (
    id          BIGSERIAL      PRIMARY KEY,
    wave_id     BIGINT         NOT NULL,
    outbound_id BIGINT         NOT NULL,
    CONSTRAINT fk_wave_order_wave FOREIGN KEY (wave_id)     REFERENCES wms_wave (id) ON DELETE CASCADE,
    CONSTRAINT fk_wave_order_order FOREIGN KEY (outbound_id) REFERENCES wms_outbound_order (id)
);
COMMENT ON TABLE wms_wave_order IS '波次包含的订单';
CREATE INDEX idx_wave_order_wave ON wms_wave_order (wave_id);

-- ---- 拣货任务 ----
CREATE TABLE wms_pick_task (
    id                BIGSERIAL      PRIMARY KEY,
    task_no           VARCHAR(32)    NOT NULL,
    wave_id           BIGINT,
    outbound_id       BIGINT         NOT NULL,
    allocation_id     BIGINT         NOT NULL,
    sku_id            BIGINT         NOT NULL,
    from_location_id  BIGINT         NOT NULL,
    sequence          INTEGER        NOT NULL DEFAULT 0,
    required_qty      NUMERIC(18,4)  NOT NULL,
    picked_qty        NUMERIC(18,4)  NOT NULL DEFAULT 0,
    status            SMALLINT       NOT NULL DEFAULT 1,
    operator          VARCHAR(64),
    picked_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_pick_task_no UNIQUE (task_no),
    CONSTRAINT fk_pick_task_outbound  FOREIGN KEY (outbound_id)  REFERENCES wms_outbound_order (id),
    CONSTRAINT fk_pick_task_allocation FOREIGN KEY (allocation_id) REFERENCES wms_allocation (id),
    CONSTRAINT ck_pick_task_qty CHECK (required_qty > 0 AND picked_qty >= 0 AND picked_qty <= required_qty)
);
COMMENT ON TABLE  wms_pick_task              IS '拣货任务';
COMMENT ON COLUMN wms_pick_task.status       IS '状态: 1=待拣货 2=拣货中 3=已完成 4=已取消';
COMMENT ON COLUMN wms_pick_task.sequence     IS '拣货顺序，同波次内按此升序作业';

CREATE INDEX idx_pick_task_wave  ON wms_pick_task (wave_id, sequence);
CREATE INDEX idx_pick_task_order ON wms_pick_task (outbound_id, status);

-- ---- 发货单 ----
CREATE TABLE wms_shipment (
    id            BIGSERIAL      PRIMARY KEY,
    shipment_no   VARCHAR(32)    NOT NULL,
    outbound_id   BIGINT         NOT NULL,
    carrier       VARCHAR(64),
    tracking_no   VARCHAR(64),
    shipped_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    operator      VARCHAR(64),
    remark        VARCHAR(255),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_shipment_no UNIQUE (shipment_no),
    CONSTRAINT fk_shipment_outbound FOREIGN KEY (outbound_id) REFERENCES wms_outbound_order (id)
);
COMMENT ON TABLE  wms_shipment IS '发货单';
CREATE INDEX idx_shipment_outbound ON wms_shipment (outbound_id);

-- =====================================================================
-- 6. 盘点
-- =====================================================================
CREATE TABLE wms_stocktake_order (
    id               BIGSERIAL      PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL,
    warehouse_id     BIGINT         NOT NULL,
    stocktake_type   SMALLINT       NOT NULL DEFAULT 1,
    status           SMALLINT       NOT NULL DEFAULT 1,
    scope_zone       VARCHAR(32),
    total_locations  INTEGER        NOT NULL DEFAULT 0,
    counted_locations INTEGER       NOT NULL DEFAULT 0,
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    approved_at      TIMESTAMPTZ,
    approver         VARCHAR(64),
    remark           VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted          SMALLINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_stocktake_order_no UNIQUE (order_no),
    CONSTRAINT fk_stocktake_warehouse FOREIGN KEY (warehouse_id) REFERENCES wms_warehouse (id)
);
COMMENT ON TABLE  wms_stocktake_order                 IS '盘点单';
COMMENT ON COLUMN wms_stocktake_order.stocktake_type  IS '类型: 1=全盘 2=循环盘 3=抽盘';
COMMENT ON COLUMN wms_stocktake_order.status           IS '状态: 1=草稿 2=进行中 3=待审批 4=已完成 5=已取消';
COMMENT ON COLUMN wms_stocktake_order.scope_zone       IS '盘点范围库区，NULL=整仓';

CREATE INDEX idx_stocktake_wh     ON wms_stocktake_order (warehouse_id, deleted);
CREATE INDEX idx_stocktake_status ON wms_stocktake_order (status);

CREATE TABLE wms_stocktake_item (
    id             BIGSERIAL      PRIMARY KEY,
    stocktake_id   BIGINT         NOT NULL,
    location_id    BIGINT         NOT NULL,
    sku_id         BIGINT         NOT NULL,
    batch_no       VARCHAR(64),
    inventory_id   BIGINT,
    book_qty       NUMERIC(18,4)  NOT NULL DEFAULT 0,
    counted_qty    NUMERIC(18,4),
    diff_qty       NUMERIC(18,4),
    status         SMALLINT       NOT NULL DEFAULT 1,
    counted_by     VARCHAR(64),
    counted_at     TIMESTAMPTZ,
    remark         VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_stocktake_item_order FOREIGN KEY (stocktake_id) REFERENCES wms_stocktake_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_stocktake_item_loc   FOREIGN KEY (location_id)  REFERENCES wms_location (id),
    CONSTRAINT fk_stocktake_item_sku   FOREIGN KEY (sku_id)       REFERENCES wms_sku (id),
    CONSTRAINT ck_stocktake_counted     CHECK (counted_qty IS NULL OR counted_qty >= 0)
);
COMMENT ON TABLE  wms_stocktake_item            IS '盘点明细';
COMMENT ON COLUMN wms_stocktake_item.book_qty    IS '账面数量，生成盘点单时快照';
COMMENT ON COLUMN wms_stocktake_item.counted_qty IS '实盘数量，未盘为 NULL';
COMMENT ON COLUMN wms_stocktake_item.diff_qty    IS '差异 = 实盘 - 账面，未盘为 NULL';
COMMENT ON COLUMN wms_stocktake_item.status      IS '状态: 1=待盘 2=已盘 3=待审批 4=已调账';

CREATE INDEX idx_stocktake_item_order ON wms_stocktake_item (stocktake_id, status);

CREATE TABLE wms_stocktake_adjust (
    id                BIGSERIAL      PRIMARY KEY,
    stocktake_id      BIGINT         NOT NULL,
    stocktake_item_id BIGINT         NOT NULL,
    inventory_id      BIGINT,
    sku_id            BIGINT         NOT NULL,
    location_id       BIGINT         NOT NULL,
    diff_qty          NUMERIC(18,4)  NOT NULL,
    approved_by       VARCHAR(64),
    approved_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    remark            VARCHAR(255),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_sta_order FOREIGN KEY (stocktake_id) REFERENCES wms_stocktake_order (id),
    CONSTRAINT fk_sta_item  FOREIGN KEY (stocktake_item_id) REFERENCES wms_stocktake_item (id)
);
COMMENT ON TABLE  wms_stocktake_adjust IS '盘点差异调整记录（盘盈盘亏）';
CREATE INDEX idx_sta_order ON wms_stocktake_adjust (stocktake_id);
