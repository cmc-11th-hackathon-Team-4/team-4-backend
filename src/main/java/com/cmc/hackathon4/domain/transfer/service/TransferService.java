package com.cmc.hackathon4.domain.transfer.service;

import java.time.Instant;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.transfer.converter.TransferConverter;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

	private final TransferRepository transferRepository;
	private final AssignmentRepository assignmentRepository;
	private final UserRepository userRepository;
	private final HouseMemberValidator houseMemberValidator;
	private final NotificationService notificationService;

	/** 덜어가기(#20). 같은 집 구성원이면 누구나 상대의 TODO 짐을 즉시 내 것으로 만든다. */
	@Transactional
	public TransferRelieveResponse relieve(Long userId, Long assignmentId) {
		User me = getUser(userId);
		Assignment assignment = getAssignment(assignmentId);
		houseMemberValidator.getMember(assignment.getPeriod().getHouse().getId(), userId);
		if (assignment.getAssignee().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_OWN_RELIEVE);
		}
		if (assignment.isDone()) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_ALREADY_DONE);
		}
		if (assignment.getPeriod().isEnded()) {
			throw new BusinessException(ErrorCode.PERIOD_ENDED);
		}
		if (transferRepository.existsByAssignmentIdAndStatus(assignmentId, TransferStatus.PENDING)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_REQUEST_PENDING);
		}

		User original = assignment.getAssignee();
		assignment.changeAssignee(me);
		Transfer transfer = transferRepository
				.save(Transfer.relieve(assignment, original, me, Instant.now()));
		notificationService.send(original, me, transfer, NotificationType.CHORE_RELIEVED,
				me.getName() + "가 " + assignment.getChore().getName() + "를 덜어갔어요");
		return TransferConverter.toRelieveResponse(transfer);
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private Assignment getAssignment(Long assignmentId) {
		return assignmentRepository.findWithDetailsById(assignmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
	}

}
