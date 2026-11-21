package com.cmc.hackathon4.domain.chore.controller;

import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chore", description = "집안일 API")
public interface ChoreApiDocs {

	@Operation(summary = "기본 집안일 목록 조회", description = "집안일 고르기 화면(S-04)의 선택지. 목록은 DB 가 아니라 서버 상수다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResponse<ChorePresetResponse> findPresets(User user);

	@Operation(summary = "우리 집 집안일 목록 조회", description = "집안일 관리(V5-12). 삭제되지 않은(active) 집안일만 반환한다. 구성원만 조회할 수 있다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND })
	ApiResponse<ChoreListResponse> findChores(User user, @Parameter(description = "집 ID", example = "1") Long houseId);

	@Operation(summary = "집안일 고르기 일괄 저장", description = "집안일 고르기 화면(S-04)에서 방장이 '다음'을 누를 때. WAITING 상태에서만 가능하며 기존 목록을 통째로 교체한다(보낸 names 가 최종 목록). 시작 후에는 집안일 추가 API 를 사용한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_OWNER_ONLY, ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND, ErrorCode.HOUSE_DISTRIBUTING, ErrorCode.HOUSE_ALREADY_STARTED })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "빈 목록, 이름 길이 위반, 중복 이름, 11개 이상 선택(최대 10개)")
	ApiResponse<ChoreListResponse> replaceChores(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			ChoreBulkSaveRequest request);

	@Operation(summary = "집안일 추가", description = "시작한 뒤 메인(V5-13)에서 구성원 누구나 집안일을 추가한다. 집이 ACTIVE 이면 추가한 사람의 짐으로 바로 들어가 assignment 가 채워지고, WAITING 이면 assignment 는 null. 배분 확인 중(DISTRIBUTING)에는 추가할 수 없다. 시작한 뒤 추가한 집안일은 그 기간(주)에만 유효하며 다음 배분에는 포함되지 않는다. 한도: 진행 중인 집안일은 집 전체 10개, 한 사람의 짐은 5개까지. 기간이 끝나 결산을 기다리는 동안에는 추가할 수 없다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND, ErrorCode.PERIOD_NOT_FOUND, ErrorCode.HOUSE_DISTRIBUTING })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "이름 길이 위반")
	@ApiErrorCase(code = ErrorCode.CHORE_LIMIT_EXCEEDED, when = "진행 중인 집안일이 이미 10개 (WAITING 이면 활성 집안일 수, ACTIVE 이면 남은 짐 수 기준)")
	@ApiErrorCase(code = ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED, when = "추가한 사람의 남은 짐이 이미 5개")
	@ApiErrorCase(code = ErrorCode.PERIOD_ENDED, when = "진행 중인 기간이 끝나 결산을 기다리는 중")
	@ApiErrorCase(code = ErrorCode.CHORE_DUPLICATED, when = "같은 이름의 집안일이 이미 있음")
	ApiResponse<ChoreCreateResponse> createChore(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			ChoreCreateRequest request);

	@Operation(summary = "집안일 삭제", description = "구성원 누구나 집안일을 삭제할 수 있다. 진행 중인 기간의 남은 짐은 담당자의 짐에서 빠지고, 그 짐에 걸려 있던 부탁은 취소된다. 이미 완료한 짐은 결산 기록으로 남는다. 다음 기간에 미리 배분된 짐은 함께 사라진다. 배분 확인 중(DISTRIBUTING)에는 삭제할 수 없다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
	@ApiErrorCodes({ ErrorCode.HOUSE_NOT_MEMBER, ErrorCode.HOUSE_NOT_FOUND, ErrorCode.CHORE_NOT_FOUND, ErrorCode.HOUSE_DISTRIBUTING })
	ApiResponse<Void> deleteChore(User user, @Parameter(description = "집 ID", example = "1") Long houseId,
			@Parameter(description = "집안일 ID", example = "1") Long choreId);

}
