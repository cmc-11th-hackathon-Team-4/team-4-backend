package com.cmc.hackathon4.global.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * 정수 필드에 소수(3.5)나 문자열("7")이 오면 잘라내거나 바꿔 받지 않고 요청 본문 형식 오류(COMMON-400)로 거절한다.
 * Jackson 기본값은 3.5 → 3, "7" → 7 로 조용히 바꿔 받아 1/3/7 같은 검증을 통과시킨다.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public JsonMapperBuilderCustomizer strictIntegerCoercion() {
		return builder -> builder.withCoercionConfig(LogicalType.Integer, config -> config
				.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.String, CoercionAction.Fail));
	}

}
