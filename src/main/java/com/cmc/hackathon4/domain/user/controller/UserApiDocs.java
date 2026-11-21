package com.cmc.hackathon4.domain.user.controller;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
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

}
