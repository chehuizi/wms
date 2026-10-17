package com.wms.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应包络 {@code {code, message, data}}，见契约第 1 节。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS, "ok", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
