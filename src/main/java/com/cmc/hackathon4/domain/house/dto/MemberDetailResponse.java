package com.cmc.hackathon4.domain.house.dto;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;

/** 상대 상세 조회(#18) 응답. 상대의 남은(TODO) 짐만 담는다. */
public record MemberDetailResponse(Long userId, String name, CharacterType characterType, Mood mood,
		int assignmentCount, List<MemberAssignment> assignments) {

	public record MemberAssignment(Long id, Long choreId, String name, AssignmentStatus status, boolean carriedOver,
			boolean requestPending) {
	}

}
