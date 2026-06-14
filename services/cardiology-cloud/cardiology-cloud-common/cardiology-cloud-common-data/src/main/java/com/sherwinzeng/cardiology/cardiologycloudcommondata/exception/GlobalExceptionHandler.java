package com.sherwinzeng.cardiology.cardiologycloudcommondata.exception;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatBusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleChatBusinessException(ChatBusinessException exception) {
        log.warn("业务异常: {}", exception.getMessage());
        BaseResponse<Void> body = BaseResponse.fail(exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getCode()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("请求参数错误");
        log.warn("参数校验失败: {}", message);
        BaseResponse<Void> body = BaseResponse.fail(ResponseCode.BAD_REQUEST, message);
        return ResponseEntity.status(ResponseCode.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<BaseResponse<Void>> handleValidationException(Exception exception) {
        String message = "请求参数错误";
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) exception;
            if (validException.getBindingResult().hasErrors()) {
                message = validException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        } else if (exception instanceof BindException) {
            BindException bindException = (BindException) exception;
            if (bindException.getBindingResult().hasErrors()) {
                message = bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            }
        }
        log.warn("参数校验失败: {}", message);
        BaseResponse<Void> body = BaseResponse.fail(ResponseCode.BAD_REQUEST, message);
        return ResponseEntity.status(ResponseCode.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("非法参数: {}", exception.getMessage());
        BaseResponse<Void> body = BaseResponse.fail(ResponseCode.BAD_REQUEST, exception.getMessage());
        return ResponseEntity.status(ResponseCode.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception exception) {
        log.error("系统异常", exception);
        BaseResponse<Void> body = BaseResponse.fail(ResponseCode.SERVER_ERROR, "系统繁忙，请稍后重试");
        return ResponseEntity.status(ResponseCode.SERVER_ERROR).body(body);
    }
}
