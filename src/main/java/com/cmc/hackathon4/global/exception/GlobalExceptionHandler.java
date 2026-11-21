package com.cmc.hackathon4.global.exception;

import java.util.stream.Collectors;

import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(ApiResponse.fail(errorCode, e.getMessage()));
	}

	/** 요청 값 검증(@Valid) 실패: "필드명: 사유" 를 이어 붙여 내려준다. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest()
				.body(ApiResponse.fail(ErrorCode.INVALID_INPUT, message));
	}

	/** JSON 형식이 깨졌거나 타입이 안 맞는 요청 본문. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleOthers(Exception e) {
		// 없는 URL(404), 지원하지 않는 메서드(405)처럼 스프링이 상태 코드를 정해둔 예외는 그 코드를 따른다.
		if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			int status = errorResponse.getStatusCode().value();
			ErrorCode errorCode = status == 404 ? ErrorCode.NOT_FOUND : ErrorCode.INVALID_INPUT;
			return ResponseEntity.status(status).body(ApiResponse.fail(errorCode));
		}
		log.error("Unhandled exception", e);
		return ResponseEntity.internalServerError().body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
	}

}
