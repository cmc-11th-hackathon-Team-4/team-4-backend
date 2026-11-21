package com.cmc.hackathon4.domain.transfer.converter;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;

/** Entity → 응답 DTO 변환을 모아 둔다. assignment 와 chore 를 읽으므로 트랜잭션 안에서 호출한다. */
public final class TransferConverter {

	private TransferConverter() {
	}

	public static TransferStatusResponse toStatusResponse(Transfer transfer) {
		return new TransferStatusResponse(transfer.getId(), transfer.getStatus());
	}

	public static TransferRelieveResponse toRelieveResponse(Transfer transfer) {
		Assignment assignment = transfer.getAssignment();
		return new TransferRelieveResponse(transfer.getId(), new TransferRelieveResponse.AssignmentSummary(
				assignment.getId(), assignment.getChore().getName(), assignment.getAssignee().getId()));
	}

}
