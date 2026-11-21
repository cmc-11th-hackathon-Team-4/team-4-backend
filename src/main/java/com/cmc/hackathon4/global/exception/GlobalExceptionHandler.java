package com.cmc.hackathon4.global.exception;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 예외를 ApiResponse 실패 형식으로 바꿔 내려준다.
 * 로그 규칙: BusinessException 은 남기지 않음, 그 외 4xx 는 warn, 5xx 는 error.
 * 사용자가 보낸 값(rejected value, 요청 본문)은 로그에 남기지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	// 서비스에서 의도적으로 던진 예외
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(ApiResponse.fail(errorCode, e.getMessage()));
	}

	// 서비스 등 @Validated 빈의 메서드 파라미터 검증 실패
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
		List<String> errors = e.getConstraintViolations().stream()
				.map(violation -> {
					String path = violation.getPropertyPath().toString();
					return path.substring(path.lastIndexOf('.') + 1) + ": " + violation.getMessage();
				})
				.toList();
		log.warn("[ConstraintViolationException] 파라미터 검증 실패: {}건", errors.size());
		return ResponseEntity.badRequest()
				.body(ApiResponse.fail(ErrorCode.INVALID_INPUT, String.join(", ", errors)));
	}

	// 유니크·FK·NOT NULL 등 DB 제약 위반
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		log.warn("[DataIntegrityViolationException] {}", causeDetail(e));
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ErrorCode.CONFLICT));
	}

	// 예상하지 못한 서버 에러
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleOthers(Exception e) {
		log.error("[UnexpectedException] 서버 내부 에러", e);
		return ResponseEntity.internalServerError().body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
	}

	// @RequestBody @Valid 검증 실패: "필드명: 사유" 를 이어 붙여 내려준다.
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = new ArrayList<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errors.add(error.getField() + ": " + error.getDefaultMessage()));
		e.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.add(error.getDefaultMessage()));
		// e.getMessage() 에는 사용자가 보낸 값이 들어가므로 필드명만 남긴다.
		log.warn("[MethodArgumentNotValidException] @Valid 검증 실패: fields={}",
				e.getBindingResult().getFieldErrors().stream().map(error -> error.getField()).toList());
		return fail(ErrorCode.INVALID_INPUT, String.join(", ", errors), headers);
	}

	// 컨트롤러 파라미터(@PathVariable, @RequestParam 등) 검증 실패. 클래스에 @Validated 가 없어도 스프링이 검증한다.
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (e.isForReturnValue()) {
			log.error("[HandlerMethodValidationException] 컨트롤러 반환값 검증 실패", e);
			return fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage(), headers);
		}

		List<String> errors = new ArrayList<>();
		e.getParameterValidationResults().forEach(result -> {
			if (result instanceof ParameterErrors parameterErrors) {
				parameterErrors.getFieldErrors()
						.forEach(error -> errors.add(error.getField() + ": " + error.getDefaultMessage()));
				parameterErrors.getGlobalErrors()
						.forEach(error -> errors.add(error.getDefaultMessage()));
			} else {
				String parameterName = result.getMethodParameter().getParameterName();
				result.getResolvableErrors()
						.forEach(error -> errors.add(parameterName + ": " + error.getDefaultMessage()));
			}
		});
		log.warn("[HandlerMethodValidationException] 컨트롤러 파라미터 검증 실패: {}건", errors.size());
		return fail(ErrorCode.INVALID_INPUT, String.join(", ", errors), headers);
	}

	// JSON 형식이 깨졌거나 필드 타입이 안 맞는 요청 본문
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Throwable cause = e.getMostSpecificCause();
		log.warn("[HttpMessageNotReadableException] 요청 본문 파싱 실패: cause={}", cause.getClass().getSimpleName());
		return fail(ErrorCode.INVALID_INPUT, "요청 본문 형식이 올바르지 않습니다.", headers);
	}

	// 필수 쿼리 파라미터 누락
	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status,
			WebRequest request) {
		log.warn("[MissingServletRequestParameterException] 필수 파라미터 누락: {}", e.getParameterName());
		return fail(ErrorCode.INVALID_INPUT, "필수 파라미터 '%s' 가 없습니다.".formatted(e.getParameterName()), headers);
	}

	// 경로/쿼리 파라미터 타입 불일치 (예: /users/abc 에서 Long 기대)
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String name = e instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: e.getPropertyName();
		log.warn("[{}] 타입 불일치: parameter={}", e.getClass().getSimpleName(), name);
		return fail(ErrorCode.INVALID_INPUT, "'%s' 값의 형식이 올바르지 않습니다.".formatted(name), headers);
	}

	/**
	 * 위에서 따로 처리하지 않은 스프링 MVC 예외(404 없는 URL, 405 메서드, 415 Content-Type 등)가 모두 여기로 온다.
	 * 스프링이 정한 상태 코드는 유지하고 응답 본문만 ApiResponse 형식으로 바꾼다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception e, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		if (statusCode.is5xxServerError()) {
			log.error("[{}] {}", e.getClass().getSimpleName(), statusCode, e);
		} else {
			log.warn("[{}] {}", e.getClass().getSimpleName(), statusCode);
		}
		ErrorCode errorCode = switch (statusCode.value()) {
			case 404 -> ErrorCode.NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_INPUT;
		};
		return super.handleExceptionInternal(e, ApiResponse.fail(errorCode), headers, statusCode, request);
	}

	private ResponseEntity<Object> fail(ErrorCode errorCode, String message, HttpHeaders headers) {
		return ResponseEntity.status(errorCode.getStatus())
				.headers(headers)
				.body(ApiResponse.fail(errorCode, message));
	}

	// DB 상세(쿼리, 값)는 숨기고 원인 예외 종류와 SQL 상태 코드만 남긴다.
	private static String causeDetail(Throwable e) {
		Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
		if (root instanceof SQLException sqlException) {
			return "sqlState=%s, vendorCode=%d".formatted(sqlException.getSQLState(), sqlException.getErrorCode());
		}
		return root.getClass().getSimpleName();
	}

}
