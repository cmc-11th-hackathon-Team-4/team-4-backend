package com.cmc.hackathon4.domain.period.converter;

import java.util.Comparator;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.dto.DistributionStartResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.user.entity.User;

/** Entity → 응답 DTO 변환을 모아 둔다. */
public final class PeriodConverter {

	private PeriodConverter() {
	}

	public static PeriodResponse toPeriodResponse(Period period) {
		return new PeriodResponse(period.getId(), period.getRound(), period.getStartDate(), period.getEndDate(),
				period.getStatus());
	}

	public static PeriodExtendResponse toExtendResponse(Period period) {
		return new PeriodExtendResponse(period.getId(), period.getEndDate());
	}

	public static PeriodEndResponse toEndResponse(Period period) {
		return new PeriodEndResponse(period.getId(), period.getStatus(), period.getEndedAt());
	}

	public static DistributionStartResponse toStartResponse(House house, Period period) {
		return new DistributionStartResponse(house.getId(), house.getStatus(), toPeriodResponse(period));
	}

	/** 구성원별 배분 결과. 구성원은 참여 순서, 짐은 집안일 ID 순이다. assignee 와 chore 를 읽는다. */
	public static DistributionResponse toDistributionResponse(House house, Period period, List<HouseMember> members,
			List<Assignment> assignments, Long myUserId) {
		List<DistributionResponse.Member> memberResponses = members.stream().map(member -> {
			User user = member.getUser();
			List<DistributionResponse.Chore> chores = assignments.stream()
					.filter(assignment -> assignment.getAssignee().getId().equals(user.getId()))
					.sorted(Comparator.comparing(assignment -> assignment.getChore().getId()))
					.map(assignment -> new DistributionResponse.Chore(assignment.getChore().getId(),
							assignment.getChore().getName()))
					.toList();
			return new DistributionResponse.Member(user.getId(), user.getName(), user.getCharacterType(),
					member.getRole(), user.getId().equals(myUserId), chores);
		}).toList();
		return new DistributionResponse(house.getStatus(), house.getSettlementCycle(), toPeriodResponse(period),
				assignments.size(), memberResponses);
	}

}
