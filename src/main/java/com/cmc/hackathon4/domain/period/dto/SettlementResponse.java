package com.cmc.hackathon4.domain.period.dto;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.entity.CarryChoice;
import com.cmc.hackathon4.domain.user.entity.CharacterType;

/** #30 결산 상세. 주간 결산(S-08), 한 일·못 한 일(S-09), 이월 선택(S-10), 배려왕(S-11)을 한 번에 담는다. */
public record SettlementResponse(PeriodResponse period, List<UserInfo> members, Summary summary, List<Chore> chores,
		List<MyUndone> myUndone, Kindness kindness, boolean nextPeriodStarted) {

	public record UserInfo(Long userId, String name, CharacterType characterType) {
	}

	/** helpedCount 는 기간 안의 ACCEPTED transfer 수(덜어가기 + 받아준 짐 올리기). */
	public record Summary(int totalCount, int doneCount, int undoneCount, int helpedCount) {
	}

	/** helpedBy 는 최종 담당자가 최초 담당자와 다르면 최종 담당자, 같으면 null. */
	public record Chore(Long assignmentId, String name, AssignmentStatus status, UserInfo assignee,
			HelpedBy helpedBy) {
	}

	public record HelpedBy(Long userId, String name) {
	}

	public record MyUndone(Long assignmentId, String name, CarryChoice carryChoice) {
	}

	/** kingUserIds 는 helpedCount 1위(동점이면 여러 명), 전원 0이면 빈 배열. */
	public record Kindness(List<Ranking> ranking, List<Long> kingUserIds) {
	}

	public record Ranking(Long userId, String name, CharacterType characterType, int helpedCount,
			int receivedHeartCount) {
	}

}
