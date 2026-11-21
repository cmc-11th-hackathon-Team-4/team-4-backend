package com.cmc.hackathon4.global.swagger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cmc.hackathon4.global.exception.ErrorCode;

/**
 * 이 API 에서 그 에러 코드가 "언제" 나는지 덧붙인다. 공통 메시지만으로 상황이 드러나지 않을 때만 쓴다.
 * 같은 코드를 {@link ApiErrorCodes} 에 또 적을 필요는 없다.
 *
 * <pre>
 * &#64;ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "days 가 1, 3, 7 이 아님")
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCase.List.class)
public @interface ApiErrorCase {

	ErrorCode code();

	String when();

	@Documented
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {

		ApiErrorCase[] value();

	}

}
