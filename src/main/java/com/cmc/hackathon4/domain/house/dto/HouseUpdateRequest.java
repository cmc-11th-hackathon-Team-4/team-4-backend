package com.cmc.hackathon4.domain.house.dto;

import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 보낸 필드만 수정한다. null 이면 바꾸지 않는다. */
public record HouseUpdateRequest(
		@Size(max = 15, message = "집 이름은 15자 이하여야 합니다.") @Pattern(regexp = ".*\\S.*", message = "집 이름은 공백만으로 구성할 수 없습니다.") String name,
		SettlementCycle settlementCycle) {
}
