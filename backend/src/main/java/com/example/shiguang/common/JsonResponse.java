package com.example.shiguang.common;

import java.io.Serializable;

public class JsonResponse<T> implements Serializable {
    private boolean status;
    private int code;
    private String message;
    private T data;

    public static <T> JsonResponse<T> success(T data) {
        JsonResponse<T> response = new JsonResponse<>();
        response.status = true;
        response.code = 200;
        response.message = "操作成功";
        response.data = data;
        return response;
    }

    public static <T> JsonResponse<T> success(T data, String message) {
        JsonResponse<T> response = success(data);
        response.message = message;
        return response;
    }

    public static <T> JsonResponse<T> failure(String message) {
        JsonResponse<T> response = new JsonResponse<>();
        response.status = false;
        response.code = 500;
        response.message = message;
        return response;
    }

    public static <T> JsonResponse<T> failure(int code, String message) {
        JsonResponse<T> response = new JsonResponse<>();
        response.status = false;
        response.code = code;
        response.message = message;
        return response;
    }

    public static <T> JsonResponse<T> error(String message) {
        return failure(400, message);
    }

    public boolean isStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
