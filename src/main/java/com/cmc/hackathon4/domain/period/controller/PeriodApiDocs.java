package com.cmc.hackathon4.domain.period.controller;

import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Period", description = "기간·결산·다음 주기 API")
public interface PeriodApiDocs {

	@Operation(summary = "이번 기간 늘리기", description = "방장이 진행 중인 기간의 결산일을 days(1, 3, 7) 만큼 미룬다. 방장 제외 전원에게 PERIOD_EXTENDED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연장 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "days 가 1, 3, 7 이 아님 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403) / 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404) / 진행 중인 기간 없음 (PERIOD-404)")
	ApiResponse<PeriodExtendResponse> extend(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			PeriodExtendRequest request);

	@Operation(summary = "기간 종료하기", description = "시연용. 방장이 진행 중인 기간을 ENDED 로 바꾸고 PENDING 부탁을 모두 CANCELED 처리한다. 전원(방장 포함)에게 PERIOD_ENDED 알림이 가고 다음 기간은 만들지 않는다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방장이 아님 (HOUSE-403) / 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "집 없음 (HOUSE-404) / 진행 중인 기간 없음 (PERIOD-404)")
	ApiResponse<PeriodEndResponse> end(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

}
