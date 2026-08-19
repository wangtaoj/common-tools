package com.wangtao.tools.exception;

/**
 * 用于包装检查型异常
 * 
 * @author wangtao
 * Created at 2026-08-19
 */
public class ToolRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -3180171370591440380L;

    public ToolRuntimeException(Throwable cause) {
        super(cause);
    }
}
