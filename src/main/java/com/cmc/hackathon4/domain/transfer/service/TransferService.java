package com.cmc.hackathon4.domain.transfer.service;

import java.time.Instant;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.assignment.service.AssignmentValidator;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.service.HouseLocker;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.transfer.converter.TransferConverter;
import com.cmc.hackathon4.domain.transfer.dto.TransferAcceptResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferHeartResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRequestCreateRequest;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.entity.TransferType;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

	private final TransferRepository transferRepository;
	private final AssignmentRepository assignmentRepository;
	private final UserRepository userRepository;
	private final HouseMemberValidator houseMemberValidator;
	private final HouseMemberRepository houseMemberRepository;
	private final HouseLocker houseLocker;
	private final AssignmentValidator assignmentValidator;
	private final NotificationService notificationService;
	private final NotificationRepository notificationRepository;

	/** 덜어가기(#20). 같은 집 구성원이면 누구나 상대의 TODO 짐을 즉시 내 것으로 만든다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransferRelieveResponse relieve(Long userId, Long assignmentId) {
		User me = getUser(userId);
		lockHouseOfAssignment(assignmentId);
		Assignment assignment = getAssignment(assignmentId);
		houseMemberValidator.getMember(assignment.getPeriod().getHouse().getId(), userId);
		if (assignment.getAssignee().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_OWN_RELIEVE);
		}
		assignmentValidator.validateOpen(assignment);
		if (transferRepository.existsByAssignmentIdAndStatus(assignmentId, TransferStatus.PENDING)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_REQUEST_PENDING);
		}
		assignmentValidator.validateTodoLimit(assignment.getPeriod().getId(), userId);

		User original = assignment.getAssignee();
		assignment.changeAssignee(me);
		Transfer transfer = transferRepository
				.save(Transfer.relieve(assignment, original, me, Instant.now()));
		notificationService.send(original, me, transfer, NotificationType.CHORE_RELIEVED,
				josa(me.getName(), "이", "가") + " " + josa(assignment.getChore().getName(), "을", "를") + " 덜어갔어요");
		return TransferConverter.toRelieveResponse(transfer);
	}

	/** 짐 올리기 부탁(#21). 담당자 본인이 내 TODO 짐을 같은 집의 다른 구성원에게 부탁한다. 알림은 만들지 않는다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransferStatusResponse request(Long userId, Long assignmentId, TransferRequestCreateRequest request) {
		User me = getUser(userId);
		lockHouseOfAssignment(assignmentId);
		Assignment assignment = getAssignment(assignmentId);
		if (!assignment.getAssignee().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_MINE);
		}
		assignmentValidator.validateOpen(assignment);
		if (transferRepository.existsByAssignmentIdAndStatus(assignmentId, TransferStatus.PENDING)) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_REQUEST_PENDING);
		}
		Long toUserId = request.toUserId();
		if (toUserId.equals(userId) || houseMemberRepository
				.findByHouseIdAndUserId(assignment.getPeriod().getHouse().getId(), toUserId).isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		User toUser = getUser(toUserId);
		Transfer transfer = transferRepository.save(Transfer.request(assignment, me, toUser));
		return TransferConverter.toStatusResponse(transfer);
	}

	/** 부탁 받기(#22). 받는 사람만 가능. 담당자가 나로 바뀌고 부탁한 사람에게 REQUEST_ACCEPTED 알림이 간다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransferAcceptResponse accept(Long userId, Long transferId) {
		User me = getUser(userId);
		lockHouseOfTransfer(transferId);
		Transfer transfer = getReceivedPendingTransfer(userId, transferId);
		Assignment assignment = transfer.getAssignment();
		assignmentValidator.validateTodoLimit(assignment.getPeriod().getId(), userId);
		transfer.accept(Instant.now());
		assignment.changeAssignee(me);
		notificationService.send(transfer.getFromUser(), me, transfer, NotificationType.REQUEST_ACCEPTED,
				josa(me.getName(), "이", "가") + " " + josa(assignment.getChore().getName(), "을", "를") + " 받았어요");
		return TransferConverter.toAcceptResponse(transfer);
	}

	/** 부탁 거절(#23). 받는 사람만 가능. 짐은 그대로 두고 부탁한 사람에게 REQUEST_DECLINED 알림이 간다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransferStatusResponse decline(Long userId, Long transferId) {
		User me = getUser(userId);
		lockHouseOfTransfer(transferId);
		Transfer transfer = getReceivedPendingTransfer(userId, transferId);
		transfer.decline(Instant.now());
		notificationService.send(transfer.getFromUser(), me, transfer, NotificationType.REQUEST_DECLINED,
				josa(me.getName(), "이", "가") + " " + josa(transfer.getAssignment().getChore().getName(), "은", "는") + " 다음에 받는대요");
		return TransferConverter.toStatusResponse(transfer);
	}

	/**
	 * 부탁 취소·덜어가기 되돌리기(#24). REQUEST 는 요청자(fromUser)가 PENDING 일 때만 취소한다.
	 * RELIEVE 는 가져간 사람(toUser)이 되돌리며, 짐이 아직 내 TODO 일 때만 가능하고 짐은 원래 담당자에게 돌아간다.
	 * 되돌린 이동은 CANCELED 로 남긴다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void cancel(Long userId, Long transferId) {
		lockHouseOfTransfer(transferId);
		Transfer transfer = getTransfer(transferId);
		Instant now = Instant.now();
		if (transfer.getType() == TransferType.REQUEST) {
			if (!transfer.getFromUser().getId().equals(userId)) {
				throw new BusinessException(ErrorCode.TRANSFER_NOT_MINE, "내가 보낸 부탁만 취소할 수 있습니다.");
			}
			if (!transfer.isPending()) {
				throw new BusinessException(ErrorCode.TRANSFER_ALREADY_HANDLED);
			}
			transfer.cancel(now);
			return;
		}

		if (!transfer.getToUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.TRANSFER_NOT_MINE, "내가 덜어간 짐만 되돌릴 수 있습니다.");
		}
		Assignment assignment = transfer.getAssignment();
		if (transfer.getStatus() != TransferStatus.ACCEPTED || assignment.isDone() || assignment.isDeleted()
				|| !assignment.getAssignee().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.TRANSFER_ALREADY_HANDLED);
		}
		assignmentValidator.validatePeriodActive(assignment.getPeriod());
		// 짐이 돌아가는 원래 담당자의 짐이 이미 한도라면 되돌릴 수 없다
		assignmentValidator.validateTodoLimit(assignment.getPeriod().getId(), transfer.getFromUser().getId());
		// 가져간 사람이 그 짐을 다시 누구에게 부탁해 둔 상태였다면 함께 취소한다.
		for (Transfer pending : transferRepository.findAllByAssignmentIdAndStatus(assignment.getId(),
				TransferStatus.PENDING)) {
			pending.cancel(now);
		}
		transfer.cancel(now);
		assignment.changeAssignee(transfer.getFromUser());
		// 짐이 돌아왔으니 원래 담당자의 "덜어갔어요" 알림은 더 이상 유효하지 않다 (하트도 보낼 수 없다)
		notificationRepository.findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(transfer.getFromUser().getId(),
				transferId, NotificationType.CHORE_RELIEVED).forEach(notification -> notification.markRead(now));
	}

	/** 하트로 고마움 전하기(#25). RELIEVE 이동에만, 원래 담당자(fromUser)만, 한 번만 보낼 수 있다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public TransferHeartResponse heart(Long userId, Long transferId) {
		User me = getUser(userId);
		lockHouseOfTransfer(transferId);
		Transfer transfer = getTransfer(transferId);
		if (!transfer.getFromUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.TRANSFER_NOT_MINE, "원래 담당자만 하트를 보낼 수 있습니다.");
		}
		if (transfer.getType() != TransferType.RELIEVE || transfer.getStatus() != TransferStatus.ACCEPTED
				|| transfer.isHearted()) {
			throw new BusinessException(ErrorCode.TRANSFER_ALREADY_HANDLED);
		}
		Instant now = Instant.now();
		transfer.heart(now);
		notificationService.send(transfer.getToUser(), me, transfer, NotificationType.HEART_RECEIVED,
				josa(me.getName(), "이", "가") + " 하트를 보냈어요");
		notificationRepository.findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(userId, transferId,
				NotificationType.CHORE_RELIEVED).forEach(notification -> notification.markRead(now));
		return TransferConverter.toHeartResponse(transfer);
	}

	/** 내가 받은 PENDING 부탁을 찾는다. 없으면 404, 받는 사람이 아니면 403, PENDING 이 아니면 409. */
	private Transfer getReceivedPendingTransfer(Long userId, Long transferId) {
		Transfer transfer = getTransfer(transferId);
		if (!transfer.getToUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.TRANSFER_NOT_MINE);
		}
		if (!transfer.isPending()) {
			throw new BusinessException(ErrorCode.TRANSFER_ALREADY_HANDLED);
		}
		assignmentValidator.validatePeriodActive(transfer.getAssignment().getPeriod());
		return transfer;
	}

	/**
	 * 같은 집의 짐·이동을 바꾸는 요청이 겹치지 않도록 집 행을 잠근다. 짐이나 이동 엔티티를 읽기 전에 불러야 잠금을 기다린 뒤의 최신 상태를 읽는다.
	 */
	private void lockHouseOfAssignment(Long assignmentId) {
		houseLocker.lock(assignmentRepository.findHouseIdById(assignmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND)));
	}

	private void lockHouseOfTransfer(Long transferId) {
		houseLocker.lock(transferRepository.findHouseIdById(transferId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND)));
	}

	private Transfer getTransfer(Long transferId) {
		return transferRepository.findWithDetailsById(transferId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private Assignment getAssignment(Long assignmentId) {
		return assignmentRepository.findWithDetailsById(assignmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ASSIGNMENT_NOT_FOUND));
	}

	/** 마지막 글자의 받침 여부에 따라 조사를 붙인다. 한글이 아니면 받침 없는 쪽을 쓴다. */
	private static String josa(String word, String withFinal, String withoutFinal) {
		char last = word.charAt(word.length() - 1);
		boolean hasFinal = last >= 0xAC00 && last <= 0xD7A3 && (last - 0xAC00) % 28 != 0;
		return word + (hasFinal ? withFinal : withoutFinal);
	}

}
