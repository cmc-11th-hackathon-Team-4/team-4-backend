package com.cmc.hackathon4.global.response;

import com.cmc.hackathon4.global.exception.ErrorCode;

/**
 * 모든 API 가 공통으로 쓰는 응답 형식.
 * 성공: { "success": true,  "code": "OK",         "message": "성공", "data": {...} }
 * 실패: { "success": false, "code": "COMMON-400", "message": "...",  "data": null }
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", "성공", data);
	}

	public static ApiResponse<Void> ok() {
		return ok(null);
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode) {
		return fail(errorCode, errorCode.getMessage());
	}

	public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
		return new ApiResponse<>(false, errorCode.getCode(), message, null);
	}

}
