-- 上架任务补上批次的生产日期 / 失效日期
--
-- **为什么补**：批次随货移动，效期是批次的属性（契约第 15.2 节
-- 「批次和序列号随货移动」）。原来 wms_putaway_task 只有 batch_no / serial_no，
-- 没有效期两列 → 执行上架时只能给库存行传 (null, null) →
-- 货到了存储位，效期没了。
--
-- 后果有三处，都不是「显示问题」：
--   1. FIFO 分配 `ORDER BY expire_date ASC NULLS LAST`
--      → 经上架的库存效期全 NULL → 全排最后 → 效期先出规则静默失效
--   2. 临期预警 `AND expire_date IS NOT NULL` → 临期 SKU 数恒为 0
--   3. 违反契约：批次属性没有随货移动
--
-- 上游已有数据：收货时 wms_inbound_receipt_item 记了效期，
-- 可以回填到对应的上架任务（receipt_item_id 是外键）。
ALTER TABLE wms_putaway_task ADD COLUMN produced_date DATE;
ALTER TABLE wms_putaway_task ADD COLUMN expire_date   DATE;

-- 回填历史任务：效期从收货明细取（一个收货明细对应一个上架任务）
UPDATE wms_putaway_task t
SET produced_date = r.produced_date,
    expire_date   = r.expire_date
FROM wms_inbound_receipt_item r
WHERE r.id = t.receipt_item_id
  AND (t.produced_date IS NULL OR t.expire_date IS NULL);

COMMENT ON COLUMN wms_putaway_task.produced_date IS '生产日期，随批次随货移动（上架时写进库存行）';
COMMENT ON COLUMN wms_putaway_task.expire_date   IS '失效日期，随批次随货移动（上架时写进库存行）';
