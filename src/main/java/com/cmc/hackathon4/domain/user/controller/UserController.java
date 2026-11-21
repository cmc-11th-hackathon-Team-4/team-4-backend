package com.cmc.hackathon4.domain.user.controller;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMeResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.dto.UserUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.service.UserService;
import com.cmc.hackathon4.global.auth.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserApiDocs {

	private final UserService userService;

	@Override
	@PostMapping
	public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
		return ApiResponse.ok(userService.create(request));
	}

	@Override
	@GetMapping("/me")
	public ApiResponse<UserMeResponse> getMe(@CurrentUser User user) {
		return ApiResponse.ok(userService.getMe(user.getId()));
	}

	@Override
	@PatchMapping("/me")
	public ApiResponse<UserResponse> update(@CurrentUser User user, @Valid @RequestBody UserUpdateRequest request) {
		return ApiResponse.ok(userService.update(user.getId(), request));
	}

	@Override
	@PatchMapping("/me/mood")
	public ApiResponse<UserMoodResponse> updateMood(@CurrentUser User user,
			@Valid @RequestBody UserMoodUpdateRequest request) {
		return ApiResponse.ok(userService.updateMood(user.getId(), request));
	}

}
