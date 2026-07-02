package com.cyx.aizerocode.exception;

/**
 * 抛异常工具类
 *
 * @author 25038
 * @since 2026-07-02
 */
public class ThrowUtils {

    /**
     * 条件成立则抛运行时异常
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException){
        if (condition) {
            throw runtimeException;
        }
    }


    /**
     * 条件成立则抛业务异常
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode){
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message){
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }



}
