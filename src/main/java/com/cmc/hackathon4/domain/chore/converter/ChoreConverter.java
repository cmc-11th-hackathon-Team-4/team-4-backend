package com.cmc.hackathon4.domain.chore.converter;

import java.util.List;

import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;

/** 집안일 도메인의 Entity ↔ DTO 변환을 모아 둔다. */
public final class ChoreConverter {

	private ChoreConverter() {
	}

	public static ChorePresetResponse toPresetResponse(List<String> presets) {
		return new ChorePresetResponse(presets);
	}

}
