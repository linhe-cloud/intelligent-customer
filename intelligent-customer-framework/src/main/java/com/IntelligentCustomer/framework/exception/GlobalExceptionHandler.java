package com.IntelligentCustomer.framework.exception;

import com.IntelligentCustomer.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.client.RestClientException;

// 全局处理异常
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 业务异常 -> 使用业务异常携带的状态码
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        if (e.getStatusCode() >= 500) {
            log.error("业务处理失败: {}", e.getMessage(), e);
        } else {
            log.warn("业务请求未完成: statusCode={}, message={}",
                    e.getStatusCode(), e.getMessage());
        }

        return response(e.getStatusCode(), e.getMessage());
    }

    // 参数异常 -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleParam(IllegalArgumentException e) {
        return response(400, e.getMessage());
    }

    // 请求体格式或字段校验异常 -> 400
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<?> handleRequest(Exception e) {
        return response(400, "请求参数不合法");
    }

    // 外部 HTTP 服务异常 -> 502
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<?> handleExternalService(RestClientException e) {
        log.error("外部HTTP服务调用失败", e);
        return response(502, "外部服务暂时不可用，请稍后重试");
    }

    // 数据库、Redis 等数据基础设施异常 -> 503
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDataAccess(DataAccessException e) {
        log.error("数据服务调用失败", e);
        return response(503, "数据服务暂时不可用，请稍后重试");
    }

    // 其他异常 -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        log.error("未处理的服务器异常", e);
        return response(500, "服务器内部错误");
    }

    private ResponseEntity<Map<String, Object>> response(
            int statusCode,
            String message
    ) {
        return ResponseEntity
                .status(statusCode)
                .body(Map.of(
                        "code", statusCode,
                        "message", message == null ? "请求处理失败" : message
                ));
    }
}
