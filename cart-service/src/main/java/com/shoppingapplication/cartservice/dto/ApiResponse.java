package com.shoppingapplication.cartservice.dto;
import lombok.Data;
@Data public class ApiResponse<T> { private boolean success; private String message; private T data; public ApiResponse() {} public ApiResponse(boolean s,String m,T d){success=s;message=m;data=d;} public static <T> ApiResponse<T> success(String m,T d){return new ApiResponse<>(true,m,d);} }
