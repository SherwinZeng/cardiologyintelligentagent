package com.sherwinzeng.cardiology.cardiologycloudcommondata.response;

public final class ResponseCode {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int FORBIDDEN = 403;
    public static final int SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private ResponseCode() {
    }
}
