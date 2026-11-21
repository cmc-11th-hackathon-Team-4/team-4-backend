package com.cmc.hackathon4.domain.assignment.controller;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.assignment.service.AssignmentService;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.service.TransferService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/assignments")
public class AssignmentController implements AssignmentApiDocs {

	private final AssignmentService assignmentService;
	private final TransferService transferService;

	@Override
	@PostMapping("/{assignmentId}/complete")
	public ApiResponse<AssignmentCompleteResponse> complete(@CurrentUser User user,
			@PathVariable Long assignmentId) {
		return ApiResponse.ok(assignmentService.complete(user.getId(), assignmentId));
	}

	@Override
	@PostMapping("/{assignmentId}/relieve")
	public ApiResponse<TransferRelieveResponse> relieve(@CurrentUser User user, @PathVariable Long assignmentId) {
		return ApiResponse.ok(transferService.relieve(user.getId(), assignmentId));
	}

}
