package com.cmc.hackathon4.domain.assignment.controller;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Assignment", description = "짐 API")
public interface AssignmentApiDocs {

	@Operation(summary = "짐 완료", description = "내 짐을 완료한다. 담당자 본인만 가능하며, 그 짐에 걸린 PENDING 부탁은 CANCELED 로 바뀐다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내 짐이 아님 (ASSIGNMENT-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "짐 없음 (ASSIGNMENT-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 완료된 짐 (ASSIGNMENT-409) / 종료된 기간 (PERIOD-409)")
	ApiResponse<AssignmentCompleteResponse> complete(User user,
			@Parameter(description = "짐 ID", example = "1") Long assignmentId);

	@Operation(summary = "덜어가기", description = "같은 집 구성원의 TODO 짐을 승인 없이 내 짐으로 가져온다. 원래 담당자에게 CHORE_RELIEVED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "덜어가기 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내 짐은 덜어갈 수 없음 (ASSIGNMENT-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "이 집의 구성원이 아님 (HOUSE-403-1)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "짐 없음 (ASSIGNMENT-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 완료된 짐 (ASSIGNMENT-409) / 부탁 중인 짐 (ASSIGNMENT-409-1) / 종료된 기간 (PERIOD-409)")
	ApiResponse<TransferRelieveResponse> relieve(User user,
			@Parameter(description = "덜어갈 짐 ID", example = "5") Long assignmentId);

}
