package com.cmc.hackathon4.domain.health;

import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Swagger 문서는 이 인터페이스에만 적는다. 컨트롤러는 이걸 implements 하고 매핑 어노테이션(@GetMapping 등)과 로직만 가진다.
 */
@Tag(name = "Health", description = "서버 상태 확인")
public interface HealthApiDocs {

	@Operation(summary = "서버 상태 확인", description = "서버가 떠 있는지와 서버 현재 시각(UTC)을 돌려준다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정상")
	ApiResponse<HealthResponse> health();

}
