package com.cmc.hackathon4.domain.testredis.controller;

import com.cmc.hackathon4.domain.testredis.dto.TestRedisResponse;
import com.cmc.hackathon4.domain.testredis.dto.TestRedisSaveRequest;
import com.cmc.hackathon4.domain.testredis.service.TestRedisService;

import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test-redis-keys")
public class TestRedisController implements TestRedisApiDocs {

	private final TestRedisService testRedisService;

	@Override
	@PutMapping("/{key}")
	public ApiResponse<TestRedisResponse> save(@PathVariable String key,
			@Valid @RequestBody TestRedisSaveRequest request) {
		return ApiResponse.ok(testRedisService.save(key, request));
	}

	@Override
	@GetMapping("/{key}")
	public ApiResponse<TestRedisResponse> find(@PathVariable String key) {
		return ApiResponse.ok(testRedisService.find(key));
	}

	@Override
	@DeleteMapping("/{key}")
	public ApiResponse<Void> delete(@PathVariable String key) {
		testRedisService.delete(key);
		return ApiResponse.ok();
	}

}
