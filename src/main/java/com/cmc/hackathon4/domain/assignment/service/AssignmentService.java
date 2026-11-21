package com.cmc.hackathon4.domain.assignment.service;

import java.time.Instant;

import com.cmc.hackathon4.domain.assignment.converter.AssignmentConverter;
import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

	private final AssignmentRepository assignmentRepository;
	private final TransferRepository transferRepository;
	private final HouseRepository houseRepository;

	/** 짐 완료(#19). 담당자 본인만 가능하고, 그 짐에 걸린 PENDING 부탁은 CANCELED 가 된다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public AssignmentCompleteResponse complete(Long userId, Long assignmentId) {
		// 완료와 덜어가기·부탁 받기가 겹치지 않도록 짐을 읽기 전에 집 행을 잠근다.
		Long houseId = assignmentRepository.findHouseIdById(assignmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
		houseRepository.findByIdForUpdate(houseId).orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		Assignment assignment = assignmentRepository.findWithDetailsById(assignmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
		if (!assignment.getAssignee().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_MINE);
		}
		if (assignment.isDone()) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_ALREADY_DONE);
		}
		if (assignment.getPeriod().isEnded()) {
			throw new BusinessException(ErrorCode.PERIOD_ENDED);
		}
		Instant now = Instant.now();
		assignment.complete(now);
		for (Transfer pending : transferRepository.findAllByAssignmentIdAndStatus(assignmentId,
				TransferStatus.PENDING)) {
			pending.cancel(now);
		}
		return AssignmentConverter.toResponse(assignment);
	}

}
