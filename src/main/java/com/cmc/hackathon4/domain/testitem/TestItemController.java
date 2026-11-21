package com.cmc.hackathon4.domain.testitem;

import java.util.List;

import com.cmc.hackathon4.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test-items")
public class TestItemController implements TestItemApiDocs {

	private final TestItemService testItemService;

	@Override
	@PostMapping
	public ApiResponse<TestItemResponse> create(@Valid @RequestBody TestItemCreateRequest request) {
		return ApiResponse.ok(testItemService.create(request));
	}

	@Override
	@GetMapping
	public ApiResponse<List<TestItemResponse>> findAll() {
		return ApiResponse.ok(testItemService.findAll());
	}

	@Override
	@GetMapping("/{id}")
	public ApiResponse<TestItemResponse> findById(@PathVariable Long id) {
		return ApiResponse.ok(testItemService.findById(id));
	}

	@Override
	@PutMapping("/{id}")
	public ApiResponse<TestItemResponse> update(@PathVariable Long id,
			@Valid @RequestBody TestItemUpdateRequest request) {
		return ApiResponse.ok(testItemService.update(id, request));
	}

	@Override
	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		testItemService.delete(id);
		return ApiResponse.ok();
	}

}
