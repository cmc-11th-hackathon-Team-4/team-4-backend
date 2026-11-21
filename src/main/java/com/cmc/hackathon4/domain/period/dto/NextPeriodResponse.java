package com.cmc.hackathon4.domain.period.dto;

import java.util.List;

import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 다음 주기 배분 결과. #32 만들기·다시 나누기와 #38 조회가 같은 모양을 쓴다. prepared 가 false 면 period 는 null, totalCount 는 0, members 는 빈 배열이다.
 */
public record NextPeriodResponse(boolean prepared, PeriodResponse period, int totalCount, List<Member> members) {

	public static NextPeriodResponse notPrepared() {
		return new NextPeriodResponse(false, null, 0, List.of());
	}

	/** chores 는 이월된 짐이 맨 앞이다. */
	public record Member(Long userId, String name, CharacterType characterType, MemberRole role,
			@JsonProperty("isMe") boolean isMe, List<Chore> chores) {
	}

	public record Chore(Long choreId, String name, boolean carriedOver) {
	}

}
