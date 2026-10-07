package com.IntelligentCustomer.common.exception;

import lombok.Getter;

// 业务异常
@Getter
public class BusinessException extends RuntimeException {

    private final int statusCode;

/**
 * 业务异常类的构造方法，用于创建带有自定义消息的业务异常对象
 * @param message 异常的详细信息，用于描述异常的具体原因
 */
    public BusinessException(String message) {
        // 调用另一个构造方法，传入消息和默认的错误码400
        this(message, 400);
    }

/**
 * 自定义业务异常类的构造方法
 * @param message 异常信息描述
 * @param statusCode HTTP状态码，用于表示业务错误类型
 */
    public BusinessException(String message, int statusCode) {
        super(message);  // 调用父类（Exception）的构造方法，设置异常信息
        this.statusCode = statusCode;  // 初始化HTTP状态码
    }

/**
 * 构造一个带有自定义消息和异常原因的BusinessException
 * 该构造方法会调用另一个构造方法，并设置默认的错误码为400
 *
 * @param message 异常的详细信息，用于描述异常的具体原因
 * @param cause 导致此异常的原始异常，可以为null
 */
    public BusinessException(String message, Throwable cause) {
        this(message, 400, cause);  // 调用另一个构造方法，传入消息、错误码400和原始异常原因
    }

/**
 * 自定义业务异常类的构造方法
 * @param message 异常信息描述
 * @param statusCode HTTP状态码，用于表示业务错误类型
 * @param cause 导致此异常的原始异常原因
 */
    public BusinessException(
            String message,      // 异常信息描述
            int statusCode,      // HTTP状态码，表示业务错误类型
            Throwable cause      // 导致此异常的原始异常原因
    ) {
        super(message, cause);    // 调用父类构造方法，初始化异常信息和原因
        this.statusCode = statusCode;  // 设置HTTP状态码
    }

}
