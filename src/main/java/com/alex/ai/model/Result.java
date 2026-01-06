package com.alex.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一 API 响应封装
 * 
 * @param <T> 响应数据类型
 * @author Alex
 * @since 2026-01-05
 */
@Schema(description = "统一响应对象")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应状态码
     */
    @Schema(description = "状态码", example = "200")
    private int code;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 响应时间戳
     */
    @Schema(description = "响应时间戳")
    private LocalDateTime timestamp;

    // ==================== 静态构造方法 ====================

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, LocalDateTime.now());
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, LocalDateTime.now());
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null, LocalDateTime.now());
    }

    /**
     * 失败响应（指定状态码）
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now());
    }

    /**
     * 参数错误响应
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null, LocalDateTime.now());
    }

    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null, LocalDateTime.now());
    }

    /**
     * 禁止访问响应
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null, LocalDateTime.now());
    }

    /**
     * 资源不存在响应
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null, LocalDateTime.now());
    }

    /**
     * 请求过于频繁响应
     */
    public static <T> Result<T> tooManyRequests(String message) {
        return new Result<>(429, message, null, LocalDateTime.now());
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == 200;
    }
}
