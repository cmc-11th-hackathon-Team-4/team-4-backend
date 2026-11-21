package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.dto.MemberDetailResponse;
import com.cmc.hackathon4.domain.house.service.HouseQueryService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.auth.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/houses")
public class HomeController implements HomeApiDocs {

	private final HouseQueryService houseQueryService;

	@Override
	@GetMapping("/{houseId}/home")
	public ApiResponse<HomeResponse> getHome(@PathVariable Long houseId, @CurrentUser User user) {
		return ApiResponse.ok(houseQueryService.getHome(houseId, user.getId()));
	}

	@Override
	@GetMapping("/{houseId}/members/{userId}")
	public ApiResponse<MemberDetailResponse> getMemberDetail(@PathVariable Long houseId, @PathVariable Long userId,
			@CurrentUser User user) {
		return ApiResponse.ok(houseQueryService.getMemberDetail(houseId, user.getId(), userId));
	}

}
