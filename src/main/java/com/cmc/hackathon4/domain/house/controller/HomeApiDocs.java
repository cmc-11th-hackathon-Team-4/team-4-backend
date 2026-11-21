package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "홈·짐", description = "내 메인과 구성원 상세 조회")
public interface HomeApiDocs {

	@Operation(summary = "내 메인 조회", description = "내 메인(V5-01). period 는 READY 를 제외한 가장 최근 기간이며 시작 전이면 null. "
			+ "기간이 ENDED 면 결산 대기 상태로 myAssignments 는 빈 배열. receivedRequests 는 내가 받은 PENDING 부탁, "
			+ "notifications 는 안 읽은 알림(최신순).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "집 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음 (USER-404) / 집 없음 (HOUSE-404)")
	ApiResponse<HomeResponse> getHome(@Parameter(description = "집 ID", example = "1") Long houseId, User user);

}
