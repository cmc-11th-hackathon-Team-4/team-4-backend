package com.cmc.hackathon4.domain.period.controller;

import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.service.PeriodService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PeriodController implements PeriodApiDocs {

	private final PeriodService periodService;

	@Override
	@PatchMapping("/houses/{houseId}/periods/current/extend")
	public ApiResponse<PeriodExtendResponse> extend(@CurrentUser User user, @PathVariable Long houseId,
			@Valid @RequestBody PeriodExtendRequest request) {
		return ApiResponse.ok(periodService.extend(houseId, user.getId(), request));
	}

	@Override
	@PostMapping("/houses/{houseId}/periods/current/end")
	public ApiResponse<PeriodEndResponse> end(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(periodService.end(houseId, user.getId()));
	}

}
