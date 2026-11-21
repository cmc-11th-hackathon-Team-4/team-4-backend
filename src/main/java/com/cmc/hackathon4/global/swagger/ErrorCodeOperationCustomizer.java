package com.cmc.hackathon4.global.swagger;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.cmc.hackathon4.global.exception.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link ApiErrorCodes}, {@link ApiErrorCase}, 요청 본문 유무를 보고 Swagger 에러 응답을 만든다.
 * 어노테이션은 컨트롤러가 implements 한 XxxApiDocs 인터페이스 메서드에서도 찾는다.
 */
@Component
public class ErrorCodeOperationCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		Method method = handlerMethod.getMethod();

		ApiErrorCodes codes = AnnotatedElementUtils.findMergedAnnotation(method, ApiErrorCodes.class);
		if (codes != null) {
			ErrorResponseDocs.add(operation, Arrays.asList(codes.value()));
		}
		for (ApiErrorCase errorCase : AnnotatedElementUtils.findMergedRepeatableAnnotations(method,
				ApiErrorCase.class)) {
			ErrorResponseDocs.add(operation, errorCase.code(), errorCase.when());
		}
		// 요청 본문이 있으면 JSON 형식 오류·@Valid 검증 실패가 COMMON-400 으로 나간다 (GlobalExceptionHandler)
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(RequestBody.class)) {
				ErrorResponseDocs.add(operation, ErrorCode.INVALID_INPUT, "요청 본문 형식 오류 또는 필드 검증 실패");
				break;
			}
		}
		return operation;
	}

}
