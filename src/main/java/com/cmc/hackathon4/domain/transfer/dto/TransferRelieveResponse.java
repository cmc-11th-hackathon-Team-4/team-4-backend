package com.cmc.hackathon4.domain.transfer.dto;

/** 덜어가기 결과. assignment.assigneeId 는 새 담당자(나)다. */
public record TransferRelieveResponse(Long transferId, AssignmentSummary assignment) {

	public record AssignmentSummary(Long id, String name, Long assigneeId) {
	}

}
