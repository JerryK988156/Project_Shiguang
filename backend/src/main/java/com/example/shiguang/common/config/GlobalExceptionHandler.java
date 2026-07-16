package com.example.shiguang.common.config;

import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.JsonResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public JsonResponse<Void> handleBusinessException(BusinessException ex) {
        return JsonResponse.failure(400, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public JsonResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "参数校验失败" : error.getDefaultMessage())
                .orElse("参数校验失败");
        return JsonResponse.failure(400, message);
    }

    @ExceptionHandler(NoClassDefFoundError.class)
    public JsonResponse<Void> handleNoClassDefFoundError(NoClassDefFoundError ex) {
        return JsonResponse.failure(500, "系统依赖缺失，请联系管理员");
    }

    @ExceptionHandler(Exception.class)
    public JsonResponse<Void> handleException(Exception ex) {
        return JsonResponse.failure(ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }

    @ExceptionHandler(Error.class)
    public JsonResponse<Void> handleError(Error ex) {
        return JsonResponse.failure(500, "系统内部错误");
    }
}
