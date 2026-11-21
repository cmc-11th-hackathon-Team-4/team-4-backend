package com.cmc.hackathon4.domain.period.controller;

import com.cmc.hackathon4.domain.period.dto.DistributionCreateRequest;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.dto.DistributionStartResponse;
import com.cmc.hackathon4.domain.period.service.DistributionService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.auth.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/houses/{houseId}")
public class DistributionController implements DistributionApiDocs {

	private final DistributionService distributionService;

	@Override
	@PostMapping("/distribution")
	public ApiResponse<DistributionResponse> create(@CurrentUser User user, @PathVariable Long houseId,
			@Valid @RequestBody DistributionCreateRequest request) {
		return ApiResponse.ok(distributionService.create(houseId, user.getId(), request));
	}

	@Override
	@GetMapping("/distribution")
	public ApiResponse<DistributionResponse> get(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(distributionService.get(houseId, user.getId()));
	}

	@Override
	@PostMapping("/distribution/shuffle")
	public ApiResponse<DistributionResponse> shuffle(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(distributionService.shuffle(houseId, user.getId()));
	}

	@Override
	@PostMapping("/start")
	public ApiResponse<DistributionStartResponse> start(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(distributionService.start(houseId, user.getId()));
	}

}
