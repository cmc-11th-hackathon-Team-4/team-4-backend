package com.cmc.hackathon4.domain.user.resolver;


import java.util.List;

import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.swagger.ErrorResponseDocs;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** @CurrentUser 리졸버 등록. Swagger 에는 User 파라미터 대신 X-Device-Id 헤더로 보이게 한다. */
@Configuration
@RequiredArgsConstructor
public class UserWebConfig implements WebMvcConfigurer {

	static {
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
	}

	private final CurrentUserArgumentResolver currentUserArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}

	@Bean
	public OperationCustomizer deviceIdHeaderCustomizer() {
		return (operation, handlerMethod) -> {
			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
				if (parameter.hasParameterAnnotation(CurrentUser.class)) {
					operation.addParametersItem(new HeaderParameter()
							.name(CurrentUserArgumentResolver.DEVICE_ID_HEADER)
							.description("앱이 발급해 보관하는 기기 UUID (사용자 식별)")
							.required(true)
							.schema(new StringSchema()));
					// 헤더 없음(COMMON-400), 등록되지 않은 기기(USER-404)는 모든 @CurrentUser API 에서 날 수 있다
					ErrorResponseDocs.add(operation, ErrorCode.INVALID_INPUT, "X-Device-Id 헤더 없음");
					ErrorResponseDocs.add(operation, ErrorCode.USER_NOT_FOUND, "등록되지 않은 기기");
					break;
				}
			}
			return operation;
		};
	}

}
