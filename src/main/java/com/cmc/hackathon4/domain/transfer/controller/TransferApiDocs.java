package com.cmc.hackathon4.domain.transfer.controller;

import com.cmc.hackathon4.domain.transfer.dto.TransferAcceptResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transfer", description = "짐 이동 API")
public interface TransferApiDocs {

	@Operation(summary = "부탁 받기", description = "받은 부탁을 수락한다. 받는 사람만 가능하며 짐의 담당자가 나로 바뀐다. 부탁한 사람에게 REQUEST_ACCEPTED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내가 받은 요청이 아님 (TRANSFER-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음 (TRANSFER-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 요청 (TRANSFER-409)")
	ApiResponse<TransferAcceptResponse> accept(User user,
			@Parameter(description = "부탁(transfer) ID", example = "2") Long transferId);

}
