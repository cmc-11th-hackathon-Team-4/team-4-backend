package com.cmc.hackathon4.domain.user.dto;

import com.cmc.hackathon4.domain.user.entity.Mood;
import jakarta.validation.constraints.NotNull;

public record UserMoodUpdateRequest(@NotNull(message = "기분은 필수입니다.") Mood mood) {
}
