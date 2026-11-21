package com.cmc.hackathon4.domain.chore.converter;

import java.util.List;

import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreResponse;
import com.cmc.hackathon4.domain.chore.entity.Chore;

/** 집안일 도메인의 Entity ↔ DTO 변환을 모아 둔다. */
public final class ChoreConverter {

	private ChoreConverter() {
	}

	public static ChorePresetResponse toPresetResponse(List<String> presets) {
		return new ChorePresetResponse(presets);
	}

	public static ChoreResponse toResponse(Chore chore) {
		return new ChoreResponse(chore.getId(), chore.getName());
	}

	public static ChoreListResponse toListResponse(List<Chore> chores) {
		return new ChoreListResponse(chores.stream().map(ChoreConverter::toResponse).toList());
	}

}
