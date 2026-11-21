package com.cmc.hackathon4.domain.chore.converter;

import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.chore.dto.ChoreAssignmentResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreResponse;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.house.entity.House;

/** 집안일 도메인의 Entity ↔ DTO 변환을 모아 둔다. */
public final class ChoreConverter {

	private ChoreConverter() {
	}

	public static ChorePresetResponse toPresetResponse(List<String> presets) {
		return new ChorePresetResponse(presets);
	}

	public static Chore toEntity(House house, String name) {
		return new Chore(house, name);
	}

	public static ChoreResponse toResponse(Chore chore) {
		return new ChoreResponse(chore.getId(), chore.getName());
	}

	/** assignment 가 null 이면 응답의 assignment 도 null. */
	public static ChoreCreateResponse toCreateResponse(Chore chore, Assignment assignment) {
		return new ChoreCreateResponse(toResponse(chore),
				assignment == null ? null : toAssignmentResponse(assignment));
	}

	public static ChoreAssignmentResponse toAssignmentResponse(Assignment assignment) {
		return new ChoreAssignmentResponse(assignment.getId(), assignment.getChore().getId(),
				assignment.getChore().getName(), assignment.getStatus(), assignment.isCarriedOver());
	}

	public static ChoreListResponse toListResponse(List<Chore> chores) {
		return new ChoreListResponse(chores.stream().map(ChoreConverter::toResponse).toList());
	}

}
