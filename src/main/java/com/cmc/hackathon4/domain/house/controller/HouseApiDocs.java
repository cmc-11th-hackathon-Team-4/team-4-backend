package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.dto.HouseJoinResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.domain.house.dto.HouseDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "House", description = "집 API")
public interface HouseApiDocs {

	@Operation(summary = "집 만들기", description = "만든 사람이 방장(OWNER)이 되고 집은 WAITING 으로 시작한다. 초대 코드는 영대문자+숫자 6자리.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이름 길이 위반 또는 공백만 입력 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 X-Device-Id (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 참여 중인 집이 있음 (HOUSE-409)")
	ApiResponse<HouseCreateResponse> create(User user, HouseCreateRequest request);

	@Operation(summary = "초대 코드로 참여", description = "구성원(MEMBER)으로 참여한다. WAITING·ACTIVE 인 집에 참여할 수 있고, 배분 진행 중(DISTRIBUTING)이면 참여할 수 없다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "코드 형식 오류 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 X-Device-Id (USER-404) / 초대 코드가 올바르지 않음 (HOUSE-404-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 참여 중인 집이 있음 (HOUSE-409) / 정원 초과 (HOUSE-409-1) / 배분 진행 중 (HOUSE-409-2)")
	ApiResponse<HouseJoinResponse> join(User user, HouseJoinRequest request);

	@Operation(summary = "집 정보·구성원 조회", description = "집 설정과 구성원 목록을 조회한다. 대기실은 이 API 를 폴링한다. WAITING 이면 currentPeriod·settlementCycle 이 null, DISTRIBUTING 이면 currentPeriod 가 READY 기간, ACTIVE 이면 READY 를 뺀 최신 기간.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "이 집의 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 X-Device-Id (USER-404) / 집을 찾을 수 없음 (HOUSE-404)")
	ApiResponse<HouseDetailResponse> getDetail(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

}
