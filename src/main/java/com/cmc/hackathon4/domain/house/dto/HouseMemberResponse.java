package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.user.entity.CharacterType;

public record HouseMemberResponse(Long userId, String name, CharacterType characterType, MemberRole role,
		boolean isMe) {
}
