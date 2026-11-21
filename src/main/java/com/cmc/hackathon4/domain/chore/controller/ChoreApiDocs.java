package com.cmc.hackathon4.domain.chore.controller;

import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Chore", description = "집안일 API")
public interface ChoreApiDocs {

	@Operation(summary = "기본 집안일 목록 조회", description = "집안일 고르기 화면(S-04)의 선택지. 목록은 DB 가 아니라 서버 상수다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록되지 않은 기기 (USER-404)")
	ApiResponse<ChorePresetResponse> findPresets(User user);

}
