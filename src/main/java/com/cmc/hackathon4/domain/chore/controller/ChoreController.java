package com.cmc.hackathon4.domain.chore.controller;

import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.service.ChoreService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Override
	@GetMapping("/houses/{houseId}/chores")
	public ApiResponse<ChoreListResponse> findChores(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(choreService.findChores(houseId, user.getId()));
	}

	@Override
	@PutMapping("/houses/{houseId}/chores")
	public ApiResponse<ChoreListResponse> replaceChores(@CurrentUser User user, @PathVariable Long houseId,
			@Valid @RequestBody ChoreBulkSaveRequest request) {
		return ApiResponse.ok(choreService.replaceChores(houseId, user.getId(), request));
	}

	@Override
	@PostMapping("/houses/{houseId}/chores")
	public ApiResponse<ChoreCreateResponse> createChore(@CurrentUser User user, @PathVariable Long houseId,
			@Valid @RequestBody ChoreCreateRequest request) {
		return ApiResponse.ok(choreService.createChore(houseId, user.getId(), request));
	}

}
