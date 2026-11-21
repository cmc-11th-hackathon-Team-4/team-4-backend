package com.cmc.hackathon4.domain.testredis;

import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "[TEST] Redis", description = "[TEST] Redis 연결 확인용 임시 API. 실제 기능이 아니며 추후 삭제 예정")
public interface TestRedisApiDocs {

	@Operation(summary = "[TEST] Redis 키 저장", description = "[TEST] 임시 테스트용. 값을 저장한다 (같은 키면 덮어씀). ttlSeconds 를 주면 그 시간 뒤 만료, 생략하면 만료 없음.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
	ApiResponse<TestRedisResponse> save(@Parameter(description = "키", example = "hello") String key,
			TestRedisSaveRequest request);

	@Operation(summary = "[TEST] Redis 키 조회", description = "[TEST] 임시 테스트용. 값과 남은 TTL(초, 만료 없음이면 -1)을 돌려준다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "키 없음 또는 만료 (TEST-REDIS-404)")
	ApiResponse<TestRedisResponse> find(@Parameter(description = "키", example = "hello") String key);

	@Operation(summary = "[TEST] Redis 키 삭제", description = "[TEST] 임시 테스트용.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "키 없음 (TEST-REDIS-404)")
	ApiResponse<Void> delete(@Parameter(description = "키", example = "hello") String key);

}
