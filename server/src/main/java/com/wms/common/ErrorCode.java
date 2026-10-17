package com.wms.common;

/**
 * 全局错误码。取值与 docs/API_CONTRACT.md 第 9 节错误码表严格一致。
 */
public final class ErrorCode {

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 参数校验失败 */
    public static final int PARAM_INVALID = 40001;
    /** 邮箱格式非法 */
    public static final int EMAIL_INVALID = 40002;
    /** 关联资源不存在或已停用 */
    public static final int RELATED_INVALID = 40003;

    /** 资源不存在或已删除 */
    public static final int NOT_FOUND = 40401;

    /** 唯一性冲突（编码重复） */
    public static final int DUPLICATE_CODE = 40901;
    /** 存在关联资源，无法删除 */
    public static final int HAS_RELATION = 40902;
    /** 状态流转不合法（如对已取消的单据收货） */
    public static final int STATUS_TRANSITION_INVALID = 40004;
    /** 盘点明细未录全就结束 */
    public static final int STOCKTAKE_INCOMPLETE = 40005;
    /** 超收或库存不足 */
    public static final int QTY_NOT_ENOUGH = 40903;
    /** 库位不允许负库存 */
    public static final int NEGATIVE_NOT_ALLOWED = 40904;
    /** 单据状态不允许该操作 */
    public static final int ORDER_STATUS_NOT_ALLOWED = 40905;
    /** 序列号重复或状态不允许 */
    public static final int SERIAL_INVALID = 40906;

    /** 服务端内部错误 */
    public static final int INTERNAL_ERROR = 50000;

    private ErrorCode() {
    }
}
