package com.cmc.hackathon4.domain.assignment.controller;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRequestCreateRequest;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Assignment", description = "짐 API")
public interface AssignmentApiDocs {

	@Operation(summary = "짐 완료", description = "내 짐을 완료한다. 담당자 본인만 가능하며, 그 짐에 걸린 PENDING 부탁은 CANCELED 로 바뀐다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공")
	@ApiErrorCodes({ ErrorCode.ASSIGNMENT_NOT_MINE, ErrorCode.ASSIGNMENT_NOT_FOUND, ErrorCode.ASSIGNMENT_ALREADY_DONE, ErrorCode.PERIOD_ENDED })
	ApiResponse<AssignmentCompleteResponse> complete(User user,
			@Parameter(description = "짐 ID", example = "1") Long assignmentId);

	@Operation(summary = "덜어가기", description = "같은 집 구성원의 TODO 짐을 승인 없이 내 짐으로 가져온다. 원래 담당자에게 CHORE_RELIEVED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "덜어가기 성공")
	@ApiErrorCodes({ ErrorCode.ASSIGNMENT_OWN_RELIEVE, ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.ASSIGNMENT_NOT_FOUND, ErrorCode.ASSIGNMENT_ALREADY_DONE, ErrorCode.ASSIGNMENT_REQUEST_PENDING, ErrorCode.PERIOD_ENDED })
	ApiResponse<TransferRelieveResponse> relieve(User user,
			@Parameter(description = "덜어갈 짐 ID", example = "5") Long assignmentId);

	@Operation(summary = "짐 올리기 부탁", description = "내 TODO 짐을 같은 집의 다른 구성원에게 부탁한다. PENDING transfer 가 만들어지고 받는 사람의 홈 receivedRequests 에 나타난다. 짐당 PENDING 은 하나만 가능하다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "부탁 성공")
	@ApiErrorCodes({ ErrorCode.ASSIGNMENT_NOT_MINE, ErrorCode.ASSIGNMENT_NOT_FOUND, ErrorCode.ASSIGNMENT_ALREADY_DONE, ErrorCode.ASSIGNMENT_REQUEST_PENDING, ErrorCode.PERIOD_ENDED })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "toUserId 가 없거나 본인이거나 같은 집 구성원이 아님")
	ApiResponse<TransferStatusResponse> request(User user,
			@Parameter(description = "부탁할 내 짐 ID", example = "3") Long assignmentId,
			TransferRequestCreateRequest request);

}
