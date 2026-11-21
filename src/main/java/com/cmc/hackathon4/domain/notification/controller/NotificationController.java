package com.cmc.hackathon4.domain.notification.controller;

import com.cmc.hackathon4.domain.notification.dto.NotificationListResponse;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.resolver.CurrentUser;
import com.cmc.hackathon4.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApiDocs {

	private final NotificationService notificationService;

	@Override
	@PatchMapping("/{notificationId}/read")
	public ApiResponse<Void> markRead(@CurrentUser User user, @PathVariable Long notificationId) {
		notificationService.markRead(user.getId(), notificationId);
		return ApiResponse.ok();
	}

	@Override
	@GetMapping
	public ApiResponse<NotificationListResponse> findMine(@CurrentUser User user,
			@RequestParam(defaultValue = "true") boolean unreadOnly, @RequestParam(required = false) Long afterId) {
		return ApiResponse.ok(new NotificationListResponse(notificationService.findMine(user.getId(), unreadOnly, afterId)));
	}

}
