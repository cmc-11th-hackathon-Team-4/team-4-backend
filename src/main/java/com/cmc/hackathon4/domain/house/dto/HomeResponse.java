package com.cmc.hackathon4.domain.house.dto;

import java.time.Instant;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodResponse;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;

/** 내 메인 조회(#17) 응답. period 는 시작 전이면 null. */
public record HomeResponse(HouseInfo house, PeriodResponse period, MeInfo me, List<MemberInfo> members,
		List<MyAssignment> myAssignments, List<ReceivedRequest> receivedRequests,
		List<NotificationResponse> notifications) {

	public record HouseInfo(Long id, String name) {
	}

	public record MeInfo(Long userId, String name, CharacterType characterType, Mood mood, int assignmentCount,
			int receivedHeartCount) {
	}

	public record MemberInfo(Long userId, String name, CharacterType characterType, Mood mood, int assignmentCount,
			boolean isMe) {
	}

	public record MyAssignment(Long id, Long choreId, String name, AssignmentStatus status, boolean carriedOver,
			PendingRequest pendingRequest) {
	}

	public record PendingRequest(Long transferId, Long toUserId, String toName) {
	}

	public record ReceivedRequest(Long transferId, Long assignmentId, String choreName, Long fromUserId,
			String fromName, Instant createdAt) {
	}

}
