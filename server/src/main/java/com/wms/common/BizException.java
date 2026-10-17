package com.wms.common;

/**
 * 业务异常。携带契约错误码与 HTTP 状态码，由 {@link GlobalExceptionHandler} 统一转换为响应包络。
 */
public class BizException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BizException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** 参数校验失败 */
    public static BizException paramInvalid(String message) {
        return new BizException(ErrorCode.PARAM_INVALID, 400, message);
    }

    /** 邮箱格式非法 */
    public static BizException emailInvalid(String message) {
        return new BizException(ErrorCode.EMAIL_INVALID, 400, message);
    }

    /** 关联资源不存在或已停用 */
    public static BizException relatedInvalid(String message) {
        return new BizException(ErrorCode.RELATED_INVALID, 400, message);
    }

    /** 资源不存在或已删除 */
    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, 404, message);
    }

    /** 唯一性冲突（编码重复） */
    public static BizException duplicateCode(String message) {
        return new BizException(ErrorCode.DUPLICATE_CODE, 409, message);
    }

    /** 存在关联资源，无法删除 */
    public static BizException hasRelation(String message) {
        return new BizException(ErrorCode.HAS_RELATION, 409, message);
    }

    /** 状态流转不合法 */
    public static BizException statusInvalid(String message) {
        return new BizException(ErrorCode.STATUS_TRANSITION_INVALID, 400, message);
    }

    /** 盘点明细未录全 */
    public static BizException stocktakeIncomplete(String message) {
        return new BizException(ErrorCode.STOCKTAKE_INCOMPLETE, 400, message);
    }

    /** 超收或库存不足 */
    public static BizException qtyNotEnough(String message) {
        return new BizException(ErrorCode.QTY_NOT_ENOUGH, 409, message);
    }

    /** 库位不允许负库存 */
    public static BizException negativeNotAllowed(String message) {
        return new BizException(ErrorCode.NEGATIVE_NOT_ALLOWED, 409, message);
    }

    /** 单据状态不允许该操作 */
    public static BizException orderStatusNotAllowed(String message) {
        return new BizException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, 409, message);
    }

    /** 序列号重复或状态不允许 */
    public static BizException serialInvalid(String message) {
        return new BizException(ErrorCode.SERIAL_INVALID, 409, message);
    }
}
