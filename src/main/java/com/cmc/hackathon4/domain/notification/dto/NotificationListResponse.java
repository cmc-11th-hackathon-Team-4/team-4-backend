package com.cmc.hackathon4.domain.notification.dto;

import java.util.List;

/** 내 알림 목록 조회 응답. */
public record NotificationListResponse(List<NotificationResponse> notifications) {
}
