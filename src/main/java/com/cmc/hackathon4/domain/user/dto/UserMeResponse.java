package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;

/** house 는 소속 집이 없으면 null. */
public record UserMeResponse(Long id, String name, CharacterType characterType, Mood mood, UserHouseResponse house) {
}
