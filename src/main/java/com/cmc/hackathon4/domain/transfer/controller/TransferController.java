package com.cmc.hackathon4.domain.transfer.controller;

import com.cmc.hackathon4.domain.transfer.dto.TransferAcceptResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
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
@RequestMapping("/api/v1/transfers")
public class TransferController implements TransferApiDocs {

	private final TransferService transferService;

	@Override
	@PostMapping("/{transferId}/accept")
	public ApiResponse<TransferAcceptResponse> accept(@CurrentUser User user, @PathVariable Long transferId) {
		return ApiResponse.ok(transferService.accept(user.getId(), transferId));
	}

	@Override
	@PostMapping("/{transferId}/decline")
	public ApiResponse<TransferStatusResponse> decline(@CurrentUser User user, @PathVariable Long transferId) {
		return ApiResponse.ok(transferService.decline(user.getId(), transferId));
	}

}
