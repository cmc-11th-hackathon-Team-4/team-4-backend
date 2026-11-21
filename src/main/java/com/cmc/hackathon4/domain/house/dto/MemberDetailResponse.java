package com.cmc.hackathon4.domain.house.dto;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;

/** 상대 상세 조회(#18) 응답. 상대의 남은(TODO) 짐만 담는다. */
public record MemberDetailResponse(Long userId, String name, CharacterType characterType, Mood mood,
		int assignmentCount, List<MemberAssignment> assignments) {

	/**
	 * 짐 한 건. requestPending 은 부탁 중이면 true 이고, pendingRequest 는 그 부탁의 받는 사람(없으면 null)이다.
	 */
	public record MemberAssignment(Long id, Long choreId, String name, AssignmentStatus status, boolean carriedOver,
			boolean requestPending, PendingRequest pendingRequest) {
	}

	/** 이 짐을 부탁한 상대(받는 사람). */
	public record PendingRequest(Long toUserId, String toName) {
	}

}
