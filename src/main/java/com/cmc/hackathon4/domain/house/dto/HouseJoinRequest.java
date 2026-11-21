package com.cmc.hackathon4.domain.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HouseJoinRequest(
		@NotBlank(message = "초대 코드는 필수입니다.") @Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대 코드는 영대문자와 숫자 6자리여야 합니다.") String inviteCode) {
}
