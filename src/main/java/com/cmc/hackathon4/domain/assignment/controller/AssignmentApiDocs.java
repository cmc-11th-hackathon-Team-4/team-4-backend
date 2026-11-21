package com.cmc.hackathon4.domain.assignment.controller;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
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

}
