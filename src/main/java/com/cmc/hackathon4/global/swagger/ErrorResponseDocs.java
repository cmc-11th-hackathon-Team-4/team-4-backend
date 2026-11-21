package com.cmc.hackathon4.global.swagger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.cmc.hackathon4.global.exception.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Swagger Operation 에 에러 응답을 더한다: HTTP 상태별로 묶고, 코드마다 실제 응답 예시를 만든다.
 * 여러 커스터마이저가 같은 Operation 에 호출해도 코드별로 한 번만 들어간다.
 */
public final class ErrorResponseDocs {

	public static final String ERROR_SCHEMA_NAME = "ErrorResponse";

	private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

	private ErrorResponseDocs() {
	}

	public static void add(Operation operation, Iterable<ErrorCode> errorCodes) {
		for (ErrorCode errorCode : errorCodes) {
			add(operation, errorCode, null);
		}
	}

	/** when: 이 API 에서 그 코드가 나는 상황 (없으면 null). 같은 코드에 여러 번 주면 " / " 로 이어 붙인다. */
	public static void add(Operation operation, ErrorCode errorCode, String when) {
		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}
		ApiResponse response = responses.computeIfAbsent(String.valueOf(errorCode.getStatus().value()),
				key -> new ApiResponse());
		MediaType mediaType = errorMediaType(response);

		Example example = mediaType.getExamples() == null ? null : mediaType.getExamples().get(errorCode.getCode());
		if (example == null) {
			example = new Example().summary(errorCode.getCode() + " " + errorCode.getMessage()).value(body(errorCode));
			mediaType.addExamples(errorCode.getCode(), example);
		}
		if (when != null && !when.isBlank() && !example.getSummary().contains(when)) {
			example.setSummary(example.getSummary() + (example.getSummary().contains(" — ") ? " / " : " — ") + when);
		}
		response.setDescription(mediaType.getExamples().values().stream()
				.map(Example::getSummary)
				.collect(Collectors.joining("<br>")));
	}

	// 에러 응답 본문은 항상 ErrorResponse 형식. 기존에 다른 스키마(성공 DTO 등)가 있었다면 바꾼다.
	private static MediaType errorMediaType(ApiResponse response) {
		MediaType mediaType = response.getContent() == null ? null : response.getContent().get(JSON);
		if (mediaType == null || mediaType.getExamples() == null) {
			mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA_NAME));
			response.setContent(new Content().addMediaType(JSON, mediaType));
		}
		return mediaType;
	}

	private static Map<String, Object> body(ErrorCode errorCode) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("code", errorCode.getCode());
		body.put("message", errorCode.getMessage());
		body.put("data", null);
		return body;
	}

}
