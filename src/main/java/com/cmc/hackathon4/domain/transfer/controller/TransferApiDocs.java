package com.cmc.hackathon4.domain.transfer.controller;

import com.cmc.hackathon4.domain.transfer.dto.TransferAcceptResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferHeartResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transfer", description = "짐 이동 API")
public interface TransferApiDocs {

	@Operation(summary = "부탁 받기", description = "받은 부탁을 수락한다. 받는 사람만 가능하며 짐의 담당자가 나로 바뀐다. 부탁한 사람에게 REQUEST_ACCEPTED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공")
	@ApiErrorCodes({ ErrorCode.TRANSFER_NOT_MINE, ErrorCode.TRANSFER_NOT_FOUND, ErrorCode.TRANSFER_ALREADY_HANDLED, ErrorCode.PERIOD_ENDED, ErrorCode.PERIOD_NOT_STARTED })
	@ApiErrorCase(code = ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED, when = "받는 사람의 남은 짐이 이미 5개")
	ApiResponse<TransferAcceptResponse> accept(User user,
			@Parameter(description = "부탁(transfer) ID", example = "2") Long transferId);

	@Operation(summary = "부탁 거절", description = "받은 부탁을 거절한다. 받는 사람만 가능하며 짐은 원래 담당자에게 그대로 남는다. 부탁한 사람에게 REQUEST_DECLINED 알림이 간다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공")
	@ApiErrorCodes({ ErrorCode.TRANSFER_NOT_MINE, ErrorCode.TRANSFER_NOT_FOUND, ErrorCode.TRANSFER_ALREADY_HANDLED, ErrorCode.PERIOD_ENDED, ErrorCode.PERIOD_NOT_STARTED })
	ApiResponse<TransferStatusResponse> decline(User user,
			@Parameter(description = "부탁(transfer) ID", example = "2") Long transferId);

	@Operation(summary = "부탁 취소·덜어가기 되돌리기", description = "PENDING 인 REQUEST 는 요청자가 취소(CANCELED)한다. RELIEVE 는 가져간 사람이 되돌리며 짐이 원래 담당자에게 돌아간다. 하트를 받은 덜어가기라면 하트도 무효가 되고 가져간 사람의 HEART_RECEIVED 알림은 읽음 처리된다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소·되돌리기 성공")
	@ApiErrorCodes({ ErrorCode.TRANSFER_NOT_FOUND, ErrorCode.TRANSFER_ALREADY_HANDLED, ErrorCode.PERIOD_ENDED, ErrorCode.PERIOD_NOT_STARTED })
	@ApiErrorCase(code = ErrorCode.TRANSFER_NOT_MINE, when = "취소·되돌릴 권한 없음")
	@ApiErrorCase(code = ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED, when = "덜어가기 되돌리기 시 원래 담당자의 남은 짐이 이미 5개")
	ApiResponse<Void> cancel(User user, @Parameter(description = "부탁(transfer) ID", example = "1") Long transferId);

	@Operation(summary = "하트로 고마움 전하기", description = "덜어가기(RELIEVE)를 한 사람에게 하트를 보낸다. 원래 담당자만, 한 번만 가능하며 덜어간 사람에게 HEART_RECEIVED 알림이 간다. 내 CHORE_RELIEVED 알림은 읽음 처리된다. 기간이 끝나면 보낼 수 없다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "하트 전송 성공")
	@ApiErrorCodes({ ErrorCode.TRANSFER_NOT_FOUND, ErrorCode.PERIOD_ENDED, ErrorCode.PERIOD_NOT_STARTED })
	@ApiErrorCase(code = ErrorCode.TRANSFER_NOT_MINE, when = "원래 담당자가 아님")
	@ApiErrorCase(code = ErrorCode.TRANSFER_ALREADY_HANDLED, when = "이미 하트를 보냈거나 덜어가기가 아님")
	ApiResponse<TransferHeartResponse> heart(User user,
			@Parameter(description = "덜어가기(transfer) ID", example = "3") Long transferId);

}
