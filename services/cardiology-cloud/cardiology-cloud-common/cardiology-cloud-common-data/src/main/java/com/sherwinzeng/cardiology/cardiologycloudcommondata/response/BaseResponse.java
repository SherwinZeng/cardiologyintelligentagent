package com.sherwinzeng.cardiology.cardiologycloudcommondata.response;

import lombok.Data;

@Data
public class BaseResponse<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(ResponseCode.SUCCESS);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> BaseResponse<T> fail(int code, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
