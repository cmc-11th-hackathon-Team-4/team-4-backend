package com.cmc.hackathon4.domain.house.converter;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.dto.MemberDetailResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.user.entity.User;

/** 홈 조회·상대 상세 조회의 Entity → 응답 DTO 변환. 연관 엔티티를 읽으므로 트랜잭션 안에서 호출한다. */
public final class HomeConverter {

	private HomeConverter() {
	}

	public static HomeResponse.HouseInfo toHouseInfo(House house) {
		return new HomeResponse.HouseInfo(house.getId(), house.getName());
	}

	public static HomeResponse.PeriodInfo toPeriodInfo(Period period) {
		return new HomeResponse.PeriodInfo(period.getId(), period.getRound(), period.getStartDate(),
				period.getEndDate(), period.getStatus());
	}

	public static HomeResponse.MeInfo toMeInfo(User user, int assignmentCount, int receivedHeartCount) {
		return new HomeResponse.MeInfo(user.getId(), user.getName(), user.getCharacterType(), user.getMood(),
				assignmentCount, receivedHeartCount);
	}

	public static HomeResponse.MemberInfo toMemberInfo(User user, int assignmentCount, boolean isMe) {
		return new HomeResponse.MemberInfo(user.getId(), user.getName(), user.getCharacterType(), user.getMood(),
				assignmentCount, isMe);
	}

	/** pendingRequest 의 받는 사람 이름은 구성원 목록에서 찾아 넘긴다(없으면 null). */
	public static HomeResponse.MyAssignment toMyAssignment(Assignment assignment, Transfer pending,
			User toUser) {
		HomeResponse.PendingRequest pendingRequest = pending == null ? null
				: new HomeResponse.PendingRequest(pending.getId(), toUser.getId(), toUser.getName());
		return new HomeResponse.MyAssignment(assignment.getId(), assignment.getChore().getId(),
				assignment.getChore().getName(), assignment.getStatus(), assignment.isCarriedOver(), pendingRequest);
	}

	public static MemberDetailResponse toMemberDetail(User user, List<MemberDetailResponse.MemberAssignment> assignments) {
		return new MemberDetailResponse(user.getId(), user.getName(), user.getCharacterType(), user.getMood(),
				assignments.size(), assignments);
	}

	public static MemberDetailResponse.MemberAssignment toMemberAssignment(Assignment assignment,
			boolean requestPending) {
		return new MemberDetailResponse.MemberAssignment(assignment.getId(), assignment.getChore().getId(),
				assignment.getChore().getName(), assignment.getStatus(), assignment.isCarriedOver(), requestPending);
	}

	public static HomeResponse.ReceivedRequest toReceivedRequest(Transfer transfer) {
		return new HomeResponse.ReceivedRequest(transfer.getId(), transfer.getAssignment().getId(),
				transfer.getAssignment().getChore().getName(), transfer.getFromUser().getId(),
				transfer.getFromUser().getName(), transfer.getCreatedAt());
	}

}
