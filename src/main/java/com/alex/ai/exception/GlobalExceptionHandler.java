package com.alex.ai.exception;

import com.alex.ai.model.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * 统一处理所有 Controller 层抛出的异常，返回标准化的响应格式
 * 
 * @author Alex
 * @since 2026-01-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity
            .status(mapCodeToHttpStatus(e.getCode()))
            .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数校验异常（@Valid 注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        
        log.warn("参数校验失败: {}", errorMessage);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.badRequest(errorMessage));
    }

    /**
     * 处理约束违反异常（@Validated 注解）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
        
        log.warn("约束违反: {}", errorMessage);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.badRequest(errorMessage));
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParamException(MissingServletRequestParameterException e) {
        String errorMessage = "缺少必需参数: " + e.getParameterName();
        log.warn(errorMessage);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.badRequest(errorMessage));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String errorMessage = "参数类型错误: " + e.getName() + " 应为 " + 
            (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型");
        log.warn(errorMessage);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.badRequest(errorMessage));
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.badRequest("请求体格式错误"));
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String errorMessage = "不支持的请求方法: " + e.getMethod();
        log.warn(errorMessage);
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Result.fail(405, errorMessage));
    }

    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFoundException(NoHandlerFoundException e) {
        String errorMessage = "接口不存在: " + e.getRequestURL();
        log.warn(errorMessage);
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Result.notFound(errorMessage));
    }

    /**
     * 处理其他所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.fail("系统繁忙，请稍后重试"));
    }

    /**
     * 将自定义错误码映射到 HTTP 状态码
     */
    private HttpStatus mapCodeToHttpStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
