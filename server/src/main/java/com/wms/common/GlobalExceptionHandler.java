package com.wms.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转换为契约第 1 节的包络与第 9 节的错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：HTTP 状态码由业务码决定 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 邮箱字段的校验失败按契约归类为 40002，其余为 40001
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            if ("Email".equals(fe.getCode())) {
                String message = fe.getField() + ": " + fe.getDefaultMessage();
                log.warn("邮箱格式校验失败: {}", message);
                return ResponseEntity.badRequest()
                        .body(Result.error(ErrorCode.EMAIL_INVALID, message));
            }
        }
        String message = describe(e.getBindingResult().getFieldErrors());
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.PARAM_INVALID, message));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = describe(e.getBindingResult().getFieldErrors());
        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.PARAM_INVALID, message));
    }

    /** 方法级约束校验失败（@Validated + @Min/@Max 等） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::describeViolation)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.PARAM_INVALID, message));
    }

    /** 查询参数类型不匹配，如 page=abc */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> handleParamTypeMismatch(Exception e) {
        String message = "请求参数格式不正确: " + e.getMessage();
        log.warn("参数类型错误: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.PARAM_INVALID, message));
    }

    /** 请求体不可解析（JSON 语法错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.PARAM_INVALID, "请求体格式不正确，无法解析 JSON"));
    }

    /**
     * 唯一约束冲突：PostgreSQL SQLState 23505。
     * 由各模块的 Service 转成更精确的 40901 文案，未捕获的落到这里兜底。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return ResponseEntity.status(409)
                .body(Result.error(ErrorCode.DUPLICATE_CODE, "编码已存在"));
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未预期的服务端异常", e);
        return ResponseEntity.status(500)
                .body(Result.error(ErrorCode.INTERNAL_ERROR, "服务端内部错误"));
    }

    private static String describe(java.util.List<FieldError> fieldErrors) {
        if (fieldErrors.isEmpty()) {
            return "参数校验失败";
        }
        return fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private static String describeViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // 路径形如 create.arg0.page，取最后一段作为字段名
        int idx = path.lastIndexOf('.');
        String field = idx >= 0 ? path.substring(idx + 1) : path;
        return field + ": " + violation.getMessage();
    }
}
