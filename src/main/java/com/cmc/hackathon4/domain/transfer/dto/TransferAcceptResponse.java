package com.cmc.hackathon4.domain.transfer.dto;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;

/** 부탁 받기 결과 */
public record TransferAcceptResponse(Long transferId, TransferStatus status, AssignmentSummary assignment) {

	public record AssignmentSummary(Long id, Long choreId, String name, AssignmentStatus status, boolean carriedOver) {
	}

}
