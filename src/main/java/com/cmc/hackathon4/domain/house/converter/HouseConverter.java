package com.cmc.hackathon4.domain.house.converter;

import java.util.List;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseDetailResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinResponse;
import com.cmc.hackathon4.domain.house.dto.HouseMemberResponse;
import com.cmc.hackathon4.domain.house.dto.HousePeriodResponse;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.period.entity.Period;

public final class HouseConverter {

	private HouseConverter() {
	}

	public static House toEntity(HouseCreateRequest request, String inviteCode) {
		return new House(request.name().trim(), inviteCode);
	}

	public static HouseCreateResponse toCreateResponse(House house) {
		return new HouseCreateResponse(house.getId(), house.getName(), house.getInviteCode(), house.getStatus());
	}

	public static HouseJoinResponse toJoinResponse(House house) {
		return new HouseJoinResponse(house.getId(), house.getName(), house.getStatus());
	}

	public static HouseUpdateResponse toUpdateResponse(House house) {
		return new HouseUpdateResponse(house.getId(), house.getName(), house.getSettlementCycle());
	}

	public static HouseMemberResponse toMemberResponse(HouseMember member, Long myUserId) {
		return new HouseMemberResponse(member.getUser().getId(), member.getUser().getName(),
				member.getUser().getCharacterType(), member.getRole(), member.getUser().getId().equals(myUserId));
	}

	/** period 가 null 이면 currentPeriod 도 null. */
	public static HouseDetailResponse toDetailResponse(House house, List<HouseMember> members, Long myUserId,
			int choreCount, Period currentPeriod) {
		List<HouseMemberResponse> memberResponses = members.stream().map(m -> toMemberResponse(m, myUserId)).toList();
		HousePeriodResponse period = currentPeriod == null ? null
				: new HousePeriodResponse(currentPeriod.getId(), currentPeriod.getRound(),
						currentPeriod.getStartDate(), currentPeriod.getEndDate(), currentPeriod.getStatus());
		return new HouseDetailResponse(house.getId(), house.getName(), house.getInviteCode(), house.getStatus(),
				house.getSettlementCycle(), members.size(), House.MAX_MEMBERS, choreCount, memberResponses, period);
	}

}
