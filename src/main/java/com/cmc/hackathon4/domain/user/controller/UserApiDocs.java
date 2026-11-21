package com.cmc.hackathon4.domain.user.controller;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMeResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.dto.UserUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.ErrorCode;
import com.cmc.hackathon4.global.response.ApiResponse;
import com.cmc.hackathon4.global.swagger.ApiErrorCase;
import com.cmc.hackathon4.global.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User", description = "사용자(프로필) API")
public interface UserApiDocs {

	@Operation(summary = "프로필 만들기", description = "앱 최초 실행 시 호출한다. 헤더 없이 본문의 deviceId 로 기기를 등록하며 기분은 RELAXED 로 시작한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
	@ApiErrorCodes({ ErrorCode.USER_ALREADY_REGISTERED })
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "이름이 앞뒤 공백을 빼면 비었거나 10자 초과(이모지도 한 글자)")
	ApiResponse<UserResponse> create(UserCreateRequest request);

	@Operation(summary = "내 정보 + 소속 집 조회", description = "앱 실행 시 첫 분기에 호출한다. 소속 집이 없으면 house 는 null 이다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResponse<UserMeResponse> getMe(User user);

	@Operation(summary = "프로필 수정", description = "이름·캐릭터를 바꾼다. 보낸 필드만 수정한다. 이름은 앞뒤 공백을 빼고 저장한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
	@ApiErrorCase(code = ErrorCode.INVALID_INPUT, when = "이름이 앞뒤 공백을 빼면 비었거나 10자 초과(이모지도 한 글자)")
	ApiResponse<UserResponse> update(User user, UserUpdateRequest request);

	@Operation(summary = "기분 변경", description = "내 기분을 바꾼다. 이전 값이 NO_CHOICE 가 아니었다가 NO_CHOICE 로 바뀌면 같은 집의 나머지 구성원 전원에게 MOOD_NO_CHOICE 알림이 생성된다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공")
	ApiResponse<UserMoodResponse> updateMood(User user, UserMoodUpdateRequest request);

}
