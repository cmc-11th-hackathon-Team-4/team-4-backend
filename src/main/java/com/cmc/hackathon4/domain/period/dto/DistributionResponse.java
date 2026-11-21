package com.cmc.hackathon4.domain.period.dto;

import java.util.List;

import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 랜덤 배분 결과. #15 만들기, #36 다시 섞기, #37 조회가 같은 모양을 쓴다. */
public record DistributionResponse(HouseStatus houseStatus, Integer settlementCycle, PeriodResponse period,
		int totalCount, List<Member> members) {

	public record Member(Long userId, String name, CharacterType characterType, MemberRole role,
			@JsonProperty("isMe") boolean isMe, List<Chore> chores) {
	}

	public record Chore(Long choreId, String name) {
	}

}
