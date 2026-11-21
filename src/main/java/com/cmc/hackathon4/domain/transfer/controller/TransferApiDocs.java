package com.cmc.hackathon4.domain.transfer.controller;

import com.cmc.hackathon4.domain.transfer.dto.TransferAcceptResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferHeartResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
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

	@Operation(summary = "부탁 거절", description = "받은 부탁을 거절한다. 받는 사람만 가능하며 짐은 원래 담당자에게 그대로 남는다. 부탁한 사람에게 REQUEST_DECLINED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내가 받은 요청이 아님 (TRANSFER-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음 (TRANSFER-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 요청 (TRANSFER-409)")
	ApiResponse<TransferStatusResponse> decline(User user,
			@Parameter(description = "부탁(transfer) ID", example = "2") Long transferId);

	@Operation(summary = "부탁 취소·덜어가기 되돌리기", description = "PENDING 인 REQUEST 는 요청자가 취소(CANCELED)한다. RELIEVE 는 가져간 사람이 되돌리며 짐이 원래 담당자에게 돌아간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소·되돌리기 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "취소·되돌릴 권한 없음 (TRANSFER-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음 (TRANSFER-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 요청 (TRANSFER-409) / 종료된 기간 (PERIOD-409)")
	ApiResponse<Void> cancel(User user, @Parameter(description = "부탁(transfer) ID", example = "1") Long transferId);

	@Operation(summary = "하트로 고마움 전하기", description = "덜어가기(RELIEVE)를 한 사람에게 하트를 보낸다. 원래 담당자만, 한 번만 가능하며 덜어간 사람에게 HEART_RECEIVED 알림이 간다. 내 CHORE_RELIEVED 알림은 읽음 처리된다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "하트 전송 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "원래 담당자가 아님 (TRANSFER-403)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음 (TRANSFER-404) / 등록되지 않은 기기 (USER-404)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 하트를 보냈거나 덜어가기가 아님 (TRANSFER-409)")
	ApiResponse<TransferHeartResponse> heart(User user,
			@Parameter(description = "덜어가기(transfer) ID", example = "3") Long transferId);

}
