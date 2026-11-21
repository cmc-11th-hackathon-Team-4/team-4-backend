package com.cmc.hackathon4.global.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.swagger.ErrorResponseDocs;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI 주소: /swagger-ui.html
@Configuration
public class SwaggerConfig {

	private static final String GUIDE = """
			## 공통 응답 형식
			성공: `{ "success": true, "code": "OK", "message": "성공", "data": { ... } }`

			실패: `{ "success": false, "code": "HOUSE-409-1", "message": "정원(5명)이 가득 찼습니다.", "data": null }`

			같은 HTTP 상태라도 원인이 여러 개일 수 있으니 **분기는 `code` 로 한다.** 각 API 의 응답 목록에서 상태별로 나올 수 있는 코드와 예시를 볼 수 있다.

			## 사용자 식별
			`X-Device-Id` 헤더(앱이 발급해 보관하는 기기 UUID)로 사용자를 찾는다. 헤더가 필요한 API 에는 파라미터로 표시된다.
			헤더가 없으면 `COMMON-400`, 등록되지 않은 기기면 `USER-404`.

			## 시간 형식
			모든 시간 값은 UTC ISO-8601 형식이다. 예: `2026-11-21T05:30:00Z`

			## 에러 코드 전체 목록
			""";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("CMC 20th 해커톤 4조 API")
						.description(GUIDE + errorCodeTable())
						.version("v1"))
				.components(new Components().addSchemas(ErrorResponseDocs.ERROR_SCHEMA_NAME, errorResponseSchema()));
	}

	// ErrorCode enum 에서 바로 만들므로 코드를 추가·수정하면 자동으로 반영된다
	private static String errorCodeTable() {
		String rows = Arrays.stream(ErrorCode.values())
				.map(errorCode -> "| `%s` | %d | %s |".formatted(errorCode.getCode(), errorCode.getStatus().value(),
						errorCode.getMessage()))
				.collect(Collectors.joining("\n"));
		return "| 코드 | HTTP | 메시지 |\n| --- | --- | --- |\n" + rows;
	}

	private static ObjectSchema errorResponseSchema() {
		ObjectSchema schema = new ObjectSchema();
		schema.description("실패 응답 (ApiResponse 실패 형식)");
		schema.addProperty("success", new BooleanSchema().example(false));
		schema.addProperty("code", new StringSchema().description("에러 코드").example("COMMON-400"));
		schema.addProperty("message", new StringSchema().description("사용자에게 보여줄 수 있는 메시지")
				.example("요청 값이 올바르지 않습니다."));
		schema.addProperty("data", new ObjectSchema().nullable(true).description("항상 null"));
		return schema;
	}

}
