package com.cmc.hackathon4.domain.testitem.converter;

import com.cmc.hackathon4.domain.testitem.dto.TestItemCreateRequest;
import com.cmc.hackathon4.domain.testitem.dto.TestItemResponse;
import com.cmc.hackathon4.domain.testitem.entity.TestItem;

/** 요청 DTO → Entity, Entity → 응답 DTO 변환을 모아 둔다. */
public final class TestItemConverter {

	private TestItemConverter() {
	}

	public static TestItem toEntity(TestItemCreateRequest request) {
		return new TestItem(request.title(), request.content());
	}

	public static TestItemResponse toResponse(TestItem item) {
		return new TestItemResponse(item.getId(), item.getTitle(), item.getContent(), item.getCreatedAt(),
				item.getUpdatedAt());
	}

}
