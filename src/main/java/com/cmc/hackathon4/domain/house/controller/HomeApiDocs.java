package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.dto.MemberDetailResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "홈·짐", description = "내 메인과 구성원 상세 조회")
public interface HomeApiDocs {

	@Operation(summary = "내 메인 조회", description = "내 메인(V5-01). period 는 READY 를 제외한 가장 최근 기간이며 시작 전이면 null. "
			+ "기간이 ENDED 면 결산 대기 상태로 myAssignments 는 빈 배열. receivedRequests 는 내가 받은 PENDING 부탁, "
			+ "notifications 는 안 읽은 알림(최신순).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.USER_NOT_FOUND, ErrorCode.HOUSE_NOT_FOUND })
	ApiResponse<HomeResponse> getHome(@Parameter(description = "집 ID", example = "1") Long houseId, User user);

	@Operation(summary = "상대 상세 조회", description = "메인 레일에서 상대를 눌렀을 때(V5-07). 상대의 남은(TODO) 짐만 반환하며, "
			+ "부탁 중인 짐은 requestPending=true 이고 pendingRequest { toUserId, toName } 에 받는 사람을 담는다(부탁 없으면 pendingRequest=null).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiErrorCodes({ ErrorCode.USER_NOT_FOUND, ErrorCode.HOUSE_NOT_FOUND, ErrorCode.PERIOD_NOT_FOUND })
	@ApiErrorCase(code = ErrorCode.HOUSE_NOT_MEMBER, when = "요청자 또는 대상이 집 구성원이 아님")
	ApiResponse<MemberDetailResponse> getMemberDetail(@Parameter(description = "집 ID", example = "1") Long houseId,
			@Parameter(description = "조회할 구성원의 사용자 ID", example = "2") Long userId, User user);

}
