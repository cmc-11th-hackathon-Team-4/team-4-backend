package com.cmc.hackathon4.global.swagger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cmc.hackathon4.global.exception.ErrorCode;

/**
 * 이 API 가 낼 수 있는 에러 코드. XxxApiDocs 메서드에 붙인다.
 * Swagger 에 HTTP 상태별 응답과 코드별 실제 응답 예시가 자동으로 만들어진다 ({@link ErrorCodeOperationCustomizer}).
 * 요청 본문 검증 실패(COMMON-400)와 X-Device-Id 관련 에러(COMMON-400, USER-404)는 자동으로 붙으므로 적지 않아도 된다.
 * 그 API 에서만의 발생 상황을 덧붙이려면 {@link ApiErrorCase} 를 쓴다.
 *
 * <pre>
 * &#64;ApiErrorCodes({ ErrorCode.TRANSFER_NOT_FOUND, ErrorCode.TRANSFER_ALREADY_HANDLED })
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

	ErrorCode[] value();

}
