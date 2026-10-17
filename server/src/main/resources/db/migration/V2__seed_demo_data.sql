-- =====================================================================
-- WMS 基础主数据 — 演示种子数据
-- 版本: V2
-- 说明: 仅供本地开发与演示，生产环境不应执行。
-- =====================================================================

INSERT INTO wms_warehouse (code, name, address, contact_name, contact_phone, status, remark) VALUES
  ('WH01', '华东中心仓', '上海市青浦区华徐公路 888 号', '张伟', '13800000001', 1, '主仓，覆盖江浙沪'),
  ('WH02', '华北中心仓', '北京市大兴区物流大道 1 号', '李娜', '13800000002', 1, NULL),
  ('WH03', '华南前置仓', '广州市白云区机场路 66 号', '王强', '13800000003', 0, '暂未启用');

INSERT INTO wms_location (warehouse_id, code, zone, location_type, capacity, allow_negative, status, remark)
SELECT w.id, v.code, v.zone, v.location_type, v.capacity, v.allow_negative, v.status, v.remark
FROM wms_warehouse w
JOIN (VALUES
  ('WH01', 'A-01-01', 'A', 1, 100.0000, 0, 1, 'A区整箱位'),
  ('WH01', 'A-01-02', 'A', 1, 100.0000, 0, 1, NULL),
  ('WH01', 'A-02-01', 'A', 1,  50.0000, 0, 1, '小件位'),
  ('WH01', 'B-01-01', 'B', 1, 200.0000, 0, 1, NULL),
  ('WH01', 'RCV-01',   'RCV', 2, NULL, 1, 1, '收货暂存区，允许负库存'),
  ('WH01', 'SHP-01',   'SHP', 3, NULL, 0, 1, '发货暂存区'),
  ('WH01', 'DEF-01',   'DEF', 4, NULL, 1, 1, '不良品区'),
  ('WH02', 'A-01-01', 'A', 1, 100.0000, 0, 1, NULL),
  ('WH02', 'A-01-02', 'A', 1, 100.0000, 0, 1, NULL)
) AS v(wh_code, code, zone, location_type, capacity, allow_negative, status, remark)
  ON w.code = v.wh_code;

INSERT INTO wms_sku (code, name, category, spec, unit, barcode, batch_managed, serial_managed, shelf_life_days, weight, volume, length, width, height, status, remark) VALUES
  ('SKU-1001', '农夫山泉 550ml 瓶装饮用水', '饮料', '550ml × 24瓶/箱', '箱', '6901285991219', 1, 0, 365, 13.5000, 0.0350, 0.4000, 0.3000, 0.2700, 1, NULL),
  ('SKU-1002', '康师傅红烧牛肉面 桶装', '方便速食', '桶装 105g', '箱', '6920202888883', 1, 0, 180, 6.2000, 0.0180, 0.4000, 0.3300, 0.1100, 1, '需批次管理'),
  ('SKU-1003', '一次性医用外科口罩 50只', '劳保防护', '50只/盒', '盒', '6971234567890', 1, 0, 1095, 0.0800, 0.0012, 0.1300, 0.1000, 0.0600, 1, NULL),
  ('SKU-1004', '小米空气净化器 4 Pro', '家用电器', '4 Pro 白色', '台', '6931234567891', 0, 1, NULL, 15.6000, 0.0980, 0.3550, 0.3550, 0.5500, 1, '序列号管理，大件'),
  ('SKU-1005', '三只松鼠每日坚果 25包', '休闲食品', '750g/箱', '箱', '6941234567892', 1, 0, 240, 3.2000, 0.0090, 0.3200, 0.2200, 0.1300, 1, NULL),
  ('SKU-1006', '折叠雨伞 自动开合', '日用百货', '五折黑色', '把', '6951234567893', 0, 0, NULL, 0.4500, 0.0035, 0.3200, 0.1200, 0.1200, 0, '已停用');

INSERT INTO wms_customer (code, name, contact_name, contact_phone, contact_email, address, status, remark) VALUES
  ('CUS001', '上海永辉超市采购部', '陈明', '13911110001', 'chenming@example.com', '上海市浦东新区张江路 1 号', 1, '商超渠道'),
  ('CUS002', '北京物美大卖场', '赵丽', '13911110002', 'zhaoli@example.com', '北京市朝阳区建国路 88 号', 1, NULL),
  ('CUS003', '广州盒马鲜生', '孙浩', '13911110003', 'sunhao@example.com', '广州市天河区体育西路 191 号', 1, '生鲜为主'),
  ('CUS004', '杭州天猫旗舰店', '周雪', '13911110004', 'zhouxue@example.com', '杭州市余杭区文一西路 969 号', 0, '已停用');
