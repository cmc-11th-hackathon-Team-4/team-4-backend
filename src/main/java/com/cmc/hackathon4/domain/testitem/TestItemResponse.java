package com.cmc.hackathon4.domain.testitem;

import java.time.Instant;

public record TestItemResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {

	public static TestItemResponse from(TestItem item) {
		return new TestItemResponse(item.getId(), item.getTitle(), item.getContent(), item.getCreatedAt(),
				item.getUpdatedAt());
	}

}
