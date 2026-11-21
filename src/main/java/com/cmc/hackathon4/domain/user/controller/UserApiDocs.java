package com.cmc.hackathon4.domain.user.controller;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMeResponse;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.dto.UserUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User", description = "사용자(프로필) API")
public interface UserApiDocs {

	@Operation(summary = "프로필 만들기", description = "앱 최초 실행 시 호출한다. 헤더 없이 본문의 deviceId 로 기기를 등록하며 기분은 RELAXED 로 시작한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 프로필이 등록된 기기 (USER-409)")
	ApiResponse<UserResponse> create(UserCreateRequest request);

	@Operation(summary = "내 정보 + 소속 집 조회", description = "앱 실행 시 첫 분기에 호출한다. 소속 집이 없으면 house 는 null 이다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 X-Device-Id (USER-404)")
	ApiResponse<UserMeResponse> getMe(User user);

	@Operation(summary = "프로필 수정", description = "이름·캐릭터를 바꾼다. 보낸 필드만 수정한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (COMMON-400)")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 X-Device-Id (USER-404)")
	ApiResponse<UserResponse> update(User user, UserUpdateRequest request);

}
