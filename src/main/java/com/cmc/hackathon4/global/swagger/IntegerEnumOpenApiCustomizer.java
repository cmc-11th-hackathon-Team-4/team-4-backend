package com.cmc.hackathon4.global.swagger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * 정수 필드의 {@code @Schema(allowableValues = {"1","3","7"})} 를 숫자 enum [1, 3, 7] 으로 바꾼다.
 * swagger-core 는 allowableValues 를 항상 문자열로 넘기는데, 서버는 정수 필드에 문자열("3")을 400 으로 거부한다(JacksonConfig).
 * 문서의 enum 을 보고 문자열을 보내는 클라이언트가 생기지 않도록 한다.
 */
@Component
public class IntegerEnumOpenApiCustomizer implements OpenApiCustomizer {

	@Override
	public void customise(OpenAPI openApi) {
		if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
			return;
		}
		openApi.getComponents().getSchemas().values().forEach(IntegerEnumOpenApiCustomizer::fix);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void fix(Schema schema) {
		if (schema == null) {
			return;
		}
		List<?> values = schema.getEnum();
		if (values != null && isInteger(schema)) {
			schema.setEnum(values.stream()
					.map(value -> value instanceof String text ? Long.valueOf(text.trim()) : value)
					.toList());
		}
		Map<String, Schema> properties = schema.getProperties();
		if (properties != null) {
			properties.values().forEach(IntegerEnumOpenApiCustomizer::fix);
		}
		fix(schema.getItems());
	}

	// OpenAPI 3.1 은 타입을 types(Set) 에, 3.0 은 type 에 둔다
	private static boolean isInteger(Schema<?> schema) {
		Set<String> types = schema.getTypes();
		return "integer".equals(schema.getType()) || (types != null && types.contains("integer"));
	}

}
