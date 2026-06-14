package com.sherwinzeng.cardiology.cardiologysession.response;

import lombok.Data;

@Data
public class BaseResponse<T> {
    private Integer code;
    private String message;
    private T data;
}
