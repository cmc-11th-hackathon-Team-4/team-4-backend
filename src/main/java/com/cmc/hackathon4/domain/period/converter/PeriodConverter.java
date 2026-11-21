package com.cmc.hackathon4.domain.period.converter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.CarryChoice;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.dto.DistributionStartResponse;
import com.cmc.hackathon4.domain.period.dto.NextPeriodResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodStartResponse;
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
	 * 1위가 0이면 king 은 비운다. 도움은 짐 하나당 최대 한 번만 센다 ({@link #creditedHelps}).
	 */
	public static SettlementResponse toSettlementResponse(Period period, List<HouseMember> members,
			List<Assignment> assignments, List<Transfer> acceptedTransfers, Long myUserId,
			boolean nextPeriodStarted) {
		List<SettlementResponse.UserInfo> memberInfos = members.stream()
				.map(member -> toUserInfo(member.getUser()))
				.toList();

		List<Transfer> credited = creditedHelps(acceptedTransfers);
		// 하트는 도움으로 인정된 사람이 그 짐에서 받은 것만, 짐당 한 번 센다
		Set<List<Long>> heartedKeys = acceptedTransfers.stream()
				.filter(Transfer::isHearted)
				.map(PeriodConverter::assignmentAndToUser)
				.collect(Collectors.toSet());

		int doneCount = (int) assignments.stream().filter(Assignment::isDone).count();
		SettlementResponse.Summary summary = new SettlementResponse.Summary(assignments.size(), doneCount,
				assignments.size() - doneCount, credited.size());

		List<SettlementResponse.Chore> chores = assignments.stream().map(assignment -> {
			User assignee = assignment.getAssignee();
			boolean helped = !assignee.getId().equals(assignment.getOriginalAssignee().getId());
			return new SettlementResponse.Chore(assignment.getId(), assignment.getChore().getName(),
					assignment.getStatus(), toUserInfo(assignee),
					helped ? new SettlementResponse.HelpedBy(assignee.getId(), assignee.getName()) : null);
		}).toList();

		List<SettlementResponse.MyUndone> myUndone = assignments.stream()
				.filter(assignment -> !assignment.isDone() && !assignment.getChore().isTemporary()
						&& assignment.getAssignee().getId().equals(myUserId))
				.map(assignment -> new SettlementResponse.MyUndone(assignment.getId(),
						assignment.getChore().getName(),
						assignment.getCarryChoice() == null ? CarryChoice.KEEP : assignment.getCarryChoice()))
				.toList();

		List<SettlementResponse.Ranking> ranking = members.stream().map(member -> {
			User user = member.getUser();
			List<Transfer> received = credited.stream()
					.filter(transfer -> transfer.getToUser().getId().equals(user.getId()))
					.toList();
			int heartCount = (int) received.stream()
					.filter(transfer -> heartedKeys.contains(assignmentAndToUser(transfer)))
					.count();
			return new SettlementResponse.Ranking(user.getId(), user.getName(), user.getCharacterType(),
					received.size(), heartCount);
		}).sorted(Comparator.comparingInt(SettlementResponse.Ranking::helpedCount).reversed()).toList();
		int top = ranking.isEmpty() ? 0 : ranking.get(0).helpedCount();
		List<Long> kingUserIds = top == 0 ? List.of()
				: ranking.stream().filter(r -> r.helpedCount() == top).map(SettlementResponse.Ranking::userId).toList();

		return new SettlementResponse(toPeriodResponse(period), memberInfos, summary, chores, myUndone,
				new SettlementResponse.Kindness(ranking, kingUserIds), nextPeriodStarted);
	}

	/**
	 * 도움으로 인정되는 이동. 짐마다 마지막(id 가 가장 큰) ACCEPTED 이동 하나만 보고, 그 받은 사람이 짐의 최초 담당자면(결국 원래 사람에게 돌아감) 세지 않는다. 그래서 덜어가기를
	 * 주고받거나 여러 번 건너가도 짐 하나는 최종적으로 맡은 한 사람에게 한 번만 인정된다.
	 */
	private static List<Transfer> creditedHelps(List<Transfer> acceptedTransfers) {
		Map<Long, Transfer> latestByAssignment = acceptedTransfers.stream()
				.collect(Collectors.toMap(transfer -> transfer.getAssignment().getId(), Function.identity(),
						BinaryOperator.maxBy(Comparator.comparing(Transfer::getId))));
		return latestByAssignment.values()
				.stream()
				.filter(transfer -> !transfer.getToUser().getId()
						.equals(transfer.getAssignment().getOriginalAssignee().getId()))
				.sorted(Comparator.comparing(Transfer::getId))
				.toList();
	}

	private static List<Long> assignmentAndToUser(Transfer transfer) {
		return List.of(transfer.getAssignment().getId(), transfer.getToUser().getId());
	}

	/** 다음 주기 배분 결과(prepared=true). 구성원은 참여 순서, 짐은 이월된 것이 먼저, 그 안에서 집안일 ID 순이다. */
	public static NextPeriodResponse toNextPeriodResponse(Period period, List<HouseMember> members,
			List<Assignment> assignments, Long myUserId) {
		List<NextPeriodResponse.Member> memberResponses = members.stream().map(member -> {
			User user = member.getUser();
			List<NextPeriodResponse.Chore> chores = assignments.stream()
					.filter(assignment -> assignment.getAssignee().getId().equals(user.getId()))
					.sorted(Comparator.comparing((Assignment assignment) -> !assignment.isCarriedOver())
							.thenComparing(assignment -> assignment.getChore().getId()))
					.map(assignment -> new NextPeriodResponse.Chore(assignment.getChore().getId(),
							assignment.getChore().getName(), assignment.isCarriedOver()))
					.toList();
			return new NextPeriodResponse.Member(user.getId(), user.getName(), user.getCharacterType(),
					member.getRole(), user.getId().equals(myUserId), chores);
		}).toList();
		return new NextPeriodResponse(true, toPeriodResponse(period), assignments.size(), memberResponses);
	}

	public static PeriodStartResponse toPeriodStartResponse(Period period) {
		return new PeriodStartResponse(toPeriodResponse(period));
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
