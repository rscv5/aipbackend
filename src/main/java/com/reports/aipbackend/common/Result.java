package com.reports.aipbackend.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    // 成功返回 200
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    // 错误返回 400
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 错误返回 400
    public static <T> Result<T> error(String message) {
        return error(400, message);
    }
} 