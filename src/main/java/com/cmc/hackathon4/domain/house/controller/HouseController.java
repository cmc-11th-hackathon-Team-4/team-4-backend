package com.cmc.hackathon4.domain.house.controller;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseDetailResponse;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.dto.HouseJoinResponse;
import com.cmc.hackathon4.domain.house.service.HouseService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/houses")
public class HouseController implements HouseApiDocs {

	private final HouseService houseService;

	@Override
	@PostMapping
	public ApiResponse<HouseCreateResponse> create(@CurrentUser User user,
			@Valid @RequestBody HouseCreateRequest request) {
		return ApiResponse.ok(houseService.create(user.getId(), request));
	}

	@Override
	@PostMapping("/join")
	public ApiResponse<HouseJoinResponse> join(@CurrentUser User user, @Valid @RequestBody HouseJoinRequest request) {
		return ApiResponse.ok(houseService.join(user.getId(), request));
	}

	@Override
	@GetMapping("/{houseId}")
	public ApiResponse<HouseDetailResponse> getDetail(@CurrentUser User user, @PathVariable Long houseId) {
		return ApiResponse.ok(houseService.getDetail(houseId, user.getId()));
	}

	@Override
	@PatchMapping("/{houseId}")
	public ApiResponse<HouseUpdateResponse> update(@CurrentUser User user, @PathVariable Long houseId,
			@Valid @RequestBody HouseUpdateRequest request) {
		return ApiResponse.ok(houseService.update(houseId, user.getId(), request));
	}

	@Override
	@DeleteMapping("/{houseId}/members/{userId}")
	public ApiResponse<Void> removeMember(@CurrentUser User user, @PathVariable Long houseId,
			@PathVariable Long userId) {
		houseService.removeMember(houseId, user.getId(), userId);
		return ApiResponse.ok();
	}

}
