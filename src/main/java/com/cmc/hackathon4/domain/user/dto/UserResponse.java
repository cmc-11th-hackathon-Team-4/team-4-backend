package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;

public record UserResponse(Long id, String name, CharacterType characterType, Mood mood) {
}
