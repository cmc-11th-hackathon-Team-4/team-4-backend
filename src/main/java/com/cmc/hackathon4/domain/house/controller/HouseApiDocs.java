package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseDetailResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.dto.HouseJoinResponse;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "House", description = "집 API")
public interface HouseApiDocs {

	@Operation(summary = "집 만들기", description = "만든 사람이 방장(OWNER)이 되고 집은 WAITING 으로 시작한다. 초대 코드는 영대문자+숫자 6자리.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_ALREADY_JOINED })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "이름 길이 위반 또는 공백만 입력")
	ApiResponse<HouseCreateResponse> create(User user, HouseCreateRequest request);

	@Operation(summary = "초대 코드로 참여", description = "구성원(MEMBER)으로 참여한다. WAITING·ACTIVE 인 집에 참여할 수 있고, 배분 진행 중(DISTRIBUTING)이면 참여할 수 없다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_INVALID_INVITE_CODE, ErrorCode.HOUSE_ALREADY_JOINED, ErrorCode.HOUSE_FULL, ErrorCode.HOUSE_DISTRIBUTING })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "코드 형식 오류")
	ApiResponse<HouseJoinResponse> join(User user, HouseJoinRequest request);

	@Operation(summary = "집 정보·구성원 조회", description = "집 설정과 구성원 목록을 조회한다. 대기실은 이 API 를 폴링한다. WAITING 이면 currentPeriod·settlementCycle 이 null, DISTRIBUTING 이면 currentPeriod 가 READY 기간, ACTIVE 이면 READY 를 뺀 최신 기간.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND })
	ApiResponse<HouseDetailResponse> getDetail(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

	@Operation(summary = "집 이름·결산 주기 수정", description = "방장만 가능. 보낸 필드만 수정하며, 결산 주기 변경은 다음 기간부터 적용된다(진행 중 기간의 endDate 는 그대로).")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_OWNER_ONLY, ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "이름 길이 위반, 주기 값 오류")
	ApiResponse<HouseUpdateResponse> update(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			HouseUpdateRequest request);

	@Operation(summary = "구성원 빼기", description = "방장만 가능하며 WAITING 상태에서만 할 수 있다. 빠진 사용자는 집이 없는 상태로 돌아간다. 방장 자신이나 이 집의 구성원이 아닌 사용자를 지정하면 COMMON-400.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내보내기 성공 (data: null)")
	@ApiErrorCodes({ ErrorCode.HOUSE_OWNER_ONLY, ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND, ErrorCode.HOUSE_DISTRIBUTING, ErrorCode.HOUSE_ALREADY_STARTED })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "방장 자신 또는 구성원이 아닌 사용자 지정")
	ApiResponse<Void> removeMember(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			@Parameter(description = "내보낼 구성원의 사용자 ID", example = "2") Long userId);

}
