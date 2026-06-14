package com.sherwinzeng.cardiology.cardiologycloudcommondata.exception;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import lombok.Getter;

@Getter
public class ChatBusinessException extends RuntimeException {

    private final int code;

    public ChatBusinessException(String message) {
        this(ResponseCode.SERVER_ERROR, message);
    }

    public ChatBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ChatBusinessException(String message, Throwable cause) {
        this(ResponseCode.SERVER_ERROR, message, cause);
    }

    public ChatBusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
