package com.cmc.hackathon4.domain.user.controller;

import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.service.UserService;
import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

}
