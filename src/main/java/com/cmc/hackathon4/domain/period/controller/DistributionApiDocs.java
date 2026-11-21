package com.cmc.hackathon4.domain.period.controller;

import com.cmc.hackathon4.domain.period.dto.DistributionCreateRequest;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.dto.DistributionStartResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Distribution", description = "온보딩 랜덤 배분 API")
public interface DistributionApiDocs {

	@Operation(summary = "결산 주기 저장·랜덤 배분 만들기", description = "방장이 결산 주기와 시작일을 저장하고 1회차 기간(READY)과 랜덤 배분을 만든다. 집은 DISTRIBUTING 이 된다. 이미 DISTRIBUTING 이면 주기·날짜를 갱신하고 배분을 새로 만든다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "시작일이 과거, 올바르지 않은 결산 주기, 집안일이 0개 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403) / 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 시작된 집 (HOUSE-409-4)")
	ApiResponse<DistributionResponse> create(User user,
			@Parameter(description = "집 ID", example = "1") Long houseId, DistributionCreateRequest request);

	@Operation(summary = "랜덤 배분 결과 조회", description = "서버에 저장된 배분 결과를 구성원 누구나 조회한다. DISTRIBUTING 일 때만 가능하다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "배분 확인 단계가 아님 (HOUSE-409-3)")
	ApiResponse<DistributionResponse> get(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

	@Operation(summary = "랜덤 배분 다시 섞기", description = "방장이 READY 기간의 배분을 지우고 랜덤 배분을 다시 만든다. DISTRIBUTING 일 때만 가능하다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "다시 섞기 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403) / 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "배분 확인 단계가 아님 (HOUSE-409-3)")
	ApiResponse<DistributionResponse> shuffle(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

	@Operation(summary = "배분 확정·우리 집 시작하기", description = "방장이 저장된 배분을 확정한다. 기간은 READY → ACTIVE, 집은 DISTRIBUTING → ACTIVE 가 되고 방장 제외 전원에게 PERIOD_STARTED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시작 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403) / 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "배분 확인 단계가 아님 (HOUSE-409-3)")
	ApiResponse<DistributionStartResponse> start(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

}
