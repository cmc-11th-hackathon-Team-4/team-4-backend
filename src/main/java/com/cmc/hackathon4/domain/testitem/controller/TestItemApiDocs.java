package com.cmc.hackathon4.domain.testitem.controller;

import java.util.List;

import com.cmc.hackathon4.domain.testitem.dto.TestItemCreateRequest;
import com.cmc.hackathon4.domain.testitem.dto.TestItemResponse;
import com.cmc.hackathon4.domain.testitem.dto.TestItemUpdateRequest;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "[TEST] Test Item", description = "[TEST] 배포·DB 연결 확인용 임시 CRUD API. 실제 기능이 아니며 추후 삭제 예정")
public interface TestItemApiDocs {

	@Operation(summary = "[TEST] 테스트 아이템 생성", description = "[TEST] 임시 테스트용. 제목(필수)과 내용으로 아이템을 만든다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
	ApiResponse<TestItemResponse> create(TestItemCreateRequest request);

	@Operation(summary = "[TEST] 테스트 아이템 목록 조회", description = "[TEST] 임시 테스트용. 전체 아이템을 조회한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResponse<List<TestItemResponse>> findAll();

	@Operation(summary = "[TEST] 테스트 아이템 단건 조회", description = "[TEST] 임시 테스트용.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템 없음 (TEST-ITEM-404)")
	ApiResponse<TestItemResponse> findById(@Parameter(description = "아이템 ID", example = "1") Long id);

	@Operation(summary = "[TEST] 테스트 아이템 수정", description = "[TEST] 임시 테스트용. 제목과 내용을 통째로 바꾼다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템 없음 (TEST-ITEM-404)")
	ApiResponse<TestItemResponse> update(@Parameter(description = "아이템 ID", example = "1") Long id,
			TestItemUpdateRequest request);

	@Operation(summary = "[TEST] 테스트 아이템 삭제", description = "[TEST] 임시 테스트용.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템 없음 (TEST-ITEM-404)")
	ApiResponse<Void> delete(@Parameter(description = "아이템 ID", example = "1") Long id);

}
