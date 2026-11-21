package com.cmc.hackathon4.domain.chore.controller;

import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chore", description = "집안일 API")
public interface ChoreApiDocs {

	@Operation(summary = "기본 집안일 목록 조회", description = "집안일 고르기 화면(S-04)의 선택지. 목록은 DB 가 아니라 서버 상수다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 기기 (USER-404)")
	ApiResponse<ChorePresetResponse> findPresets(User user);

	@Operation(summary = "우리 집 집안일 목록 조회", description = "집안일 관리(V5-12). 삭제되지 않은(active) 집안일만 반환한다. 구성원만 조회할 수 있다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "이 집의 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 기기 (USER-404), 집 없음 (HOUSE-404)")
	ApiResponse<ChoreListResponse> findChores(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

	@Operation(summary = "집안일 고르기 일괄 저장", description = "집안일 고르기 화면(S-04)에서 방장이 '다음'을 누를 때. WAITING 상태에서만 가능하며 기존 목록을 통째로 교체한다(보낸 names 가 최종 목록). 시작 후에는 집안일 추가 API 를 사용한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "빈 목록, 이름 길이 위반, 중복 이름 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403), 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 기기 (USER-404), 집 없음 (HOUSE-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "배분 진행 중 (HOUSE-409-2), 이미 시작된 집 (HOUSE-409-4)")
	ApiResponse<ChoreListResponse> replaceChores(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			ChoreBulkSaveRequest request);

}
