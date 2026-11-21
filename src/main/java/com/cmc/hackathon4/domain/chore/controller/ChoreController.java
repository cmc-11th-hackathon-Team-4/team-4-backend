package com.cmc.hackathon4.domain.chore.controller;

import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.service.ChoreService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChoreController implements ChoreApiDocs {

	private final ChoreService choreService;

	@Override
	@GetMapping("/chore-presets")
	public ApiResponse<ChorePresetResponse> findPresets(@CurrentUser User user) {
		return ApiResponse.ok(choreService.findPresets());
	}

}
