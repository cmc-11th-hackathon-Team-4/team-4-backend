package com.cmc.hackathon4.domain.period.converter;

import java.util.Comparator;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.CarryChoice;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.dto.DistributionStartResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodResponse;
import com.cmc.hackathon4.domain.period.dto.SettlementResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
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

	/**
	 * 결산 상세. assignee·originalAssignee·chore, transfer 의 toUser 를 읽는다. kindness 순위는 helpedCount 내림차순(동률은 참여 순서)이고,
	 * 1위가 0이면 king 은 비운다.
	 */
	public static SettlementResponse toSettlementResponse(Period period, List<HouseMember> members,
			List<Assignment> assignments, List<Transfer> acceptedTransfers, Long myUserId,
			boolean nextPeriodStarted) {
		List<SettlementResponse.UserInfo> memberInfos = members.stream()
				.map(member -> toUserInfo(member.getUser()))
				.toList();

		int doneCount = (int) assignments.stream().filter(Assignment::isDone).count();
		SettlementResponse.Summary summary = new SettlementResponse.Summary(assignments.size(), doneCount,
				assignments.size() - doneCount, acceptedTransfers.size());

		List<SettlementResponse.Chore> chores = assignments.stream().map(assignment -> {
			User assignee = assignment.getAssignee();
			boolean helped = !assignee.getId().equals(assignment.getOriginalAssignee().getId());
			return new SettlementResponse.Chore(assignment.getId(), assignment.getChore().getName(),
					assignment.getStatus(), toUserInfo(assignee),
					helped ? new SettlementResponse.HelpedBy(assignee.getId(), assignee.getName()) : null);
		}).toList();

		List<SettlementResponse.MyUndone> myUndone = assignments.stream()
				.filter(assignment -> !assignment.isDone() && assignment.getAssignee().getId().equals(myUserId))
				.map(assignment -> new SettlementResponse.MyUndone(assignment.getId(),
						assignment.getChore().getName(),
						assignment.getCarryChoice() == null ? CarryChoice.KEEP : assignment.getCarryChoice()))
				.toList();

		List<SettlementResponse.Ranking> ranking = members.stream().map(member -> {
			User user = member.getUser();
			List<Transfer> received = acceptedTransfers.stream()
					.filter(transfer -> transfer.getToUser().getId().equals(user.getId()))
					.toList();
			return new SettlementResponse.Ranking(user.getId(), user.getName(), user.getCharacterType(),
					received.size(), (int) received.stream().filter(Transfer::isHearted).count());
		}).sorted(Comparator.comparingInt(SettlementResponse.Ranking::helpedCount).reversed()).toList();
		int top = ranking.isEmpty() ? 0 : ranking.get(0).helpedCount();
		List<Long> kingUserIds = top == 0 ? List.of()
				: ranking.stream().filter(r -> r.helpedCount() == top).map(SettlementResponse.Ranking::userId).toList();

		return new SettlementResponse(toPeriodResponse(period), memberInfos, summary, chores, myUndone,
				new SettlementResponse.Kindness(ranking, kingUserIds), nextPeriodStarted);
	}

	private static SettlementResponse.UserInfo toUserInfo(User user) {
		return new SettlementResponse.UserInfo(user.getId(), user.getName(), user.getCharacterType());
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
