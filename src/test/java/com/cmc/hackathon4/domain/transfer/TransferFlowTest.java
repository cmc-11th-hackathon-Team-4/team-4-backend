package com.cmc.hackathon4.domain.transfer;

import java.time.Instant;
import java.time.LocalDate;

import com.cmc.hackathon4.domain.assignment.dto.AssignmentCompleteResponse;
import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.assignment.service.AssignmentService;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.dto.TransferHeartResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRelieveResponse;
import com.cmc.hackathon4.domain.transfer.dto.TransferRequestCreateRequest;
import com.cmc.hackathon4.domain.transfer.dto.TransferStatusResponse;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.transfer.service.TransferService;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 짐 완료(#19)와 짐 이동(#20~#25) 서비스의 핵심 규칙을 확인한다. */
@SpringBootTest
@Transactional
class TransferFlowTest {

	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;
	@Autowired
	HouseMemberRepository houseMemberRepository;
	@Autowired
	ChoreRepository choreRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	AssignmentRepository assignmentRepository;
	@Autowired
	TransferRepository transferRepository;
	@Autowired
	NotificationRepository notificationRepository;
	@Autowired
	AssignmentService assignmentService;
	@Autowired
	TransferService transferService;

	User suah;
	User minji;
	User outsider;
	House house;
	Period period;
	Chore chore;
	Assignment assignment;

	@BeforeEach
	void setUp() {
		suah = userRepository.save(new User("device-1", "수아", CharacterType.CHARACTER_1));
		minji = userRepository.save(new User("device-2", "민지", CharacterType.CHARACTER_3));
		outsider = userRepository.save(new User("device-3", "외부인", CharacterType.CHARACTER_2));
		house = houseRepository.save(new House("302호 우리집", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, suah, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, minji, MemberRole.MEMBER));
		chore = choreRepository.save(new Chore(house, "분리수거"));
		LocalDate start = LocalDate.of(2026, 11, 23);
		period = new Period(house, 1, start, SettlementCycle.WEEKLY.endDateFrom(start));
		period.start();
		periodRepository.save(period);
		assignment = assignmentRepository.save(new Assignment(period, chore, suah, false));
	}

	private void expectError(Runnable action, ErrorCode code) {
		assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
	}

	// ---- #19 짐 완료 ----

	@Test
	void 짐을_완료하면_DONE_이_되고_진행_중인_부탁은_취소된다() {
		TransferStatusResponse request = transferService.request(suah.getId(), assignment.getId(),
				new TransferRequestCreateRequest(minji.getId()));

		AssignmentCompleteResponse response = assignmentService.complete(suah.getId(), assignment.getId());

		assertThat(response.status()).isEqualTo(AssignmentStatus.DONE);
		assertThat(response.completedAt()).isNotNull();
		assertThat(transferRepository.findById(request.transferId()).orElseThrow().getStatus())
				.isEqualTo(TransferStatus.CANCELED);
	}

	@Test
	void 짐_완료는_담당자만_하고_두_번_할_수_없고_종료된_기간에서는_할_수_없다() {
		expectError(() -> assignmentService.complete(minji.getId(), assignment.getId()),
				ErrorCode.ASSIGNMENT_NOT_MINE);

		assignmentService.complete(suah.getId(), assignment.getId());
		expectError(() -> assignmentService.complete(suah.getId(), assignment.getId()),
				ErrorCode.ASSIGNMENT_ALREADY_DONE);

		Assignment another = assignmentRepository.save(new Assignment(period, chore, suah, false));
		period.end(java.time.Instant.now());
		expectError(() -> assignmentService.complete(suah.getId(), another.getId()), ErrorCode.PERIOD_ENDED);
	}

	// ---- #20 덜어가기 ----

	@Test
	void 덜어가면_담당자가_바뀌고_원래_담당자에게_알림이_간다() {
		TransferRelieveResponse response = transferService.relieve(minji.getId(), assignment.getId());

		assertThat(response.assignment().assigneeId()).isEqualTo(minji.getId());
		assertThat(response.assignment().name()).isEqualTo("분리수거");
		assertThat(assignment.getAssignee()).isEqualTo(minji);
		assertThat(assignment.getOriginalAssignee()).isEqualTo(suah);
		Transfer transfer = transferRepository.findById(response.transferId()).orElseThrow();
		assertThat(transfer.getStatus()).isEqualTo(TransferStatus.ACCEPTED);
		Notification notification = notificationRepository
				.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(suah.getId()).get(0);
		assertThat(notification.getType()).isEqualTo(NotificationType.CHORE_RELIEVED);
		assertThat(notification.getMessage()).isEqualTo("민지가 분리수거를 덜어갔어요");
		assertThat(notification.getTransfer().getId()).isEqualTo(transfer.getId());
	}

	@Test
	void 덜어가기_차단_조건() {
		expectError(() -> transferService.relieve(suah.getId(), assignment.getId()), ErrorCode.ASSIGNMENT_OWN_RELIEVE);
		expectError(() -> transferService.relieve(outsider.getId(), assignment.getId()), ErrorCode.HOUSE_NOT_MEMBER);

		transferService.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()));
		expectError(() -> transferService.relieve(minji.getId(), assignment.getId()),
				ErrorCode.ASSIGNMENT_REQUEST_PENDING);

		Assignment done = assignmentRepository.save(new Assignment(period, chore, suah, false));
		done.complete(java.time.Instant.now());
		expectError(() -> transferService.relieve(minji.getId(), done.getId()), ErrorCode.ASSIGNMENT_ALREADY_DONE);

		Assignment ended = assignmentRepository.save(new Assignment(period, chore, suah, false));
		period.end(java.time.Instant.now());
		expectError(() -> transferService.relieve(minji.getId(), ended.getId()), ErrorCode.PERIOD_ENDED);
	}

	@Test
	void 시작_전_기간의_짐은_완료_덜어가기_부탁을_할_수_없다() {
		LocalDate nextStart = period.getEndDate().plusDays(1);
		Period next = periodRepository
				.save(new Period(house, 2, nextStart, SettlementCycle.WEEKLY.endDateFrom(nextStart)));
		Assignment ready = assignmentRepository.save(new Assignment(next, chore, suah, false));

		expectError(() -> assignmentService.complete(suah.getId(), ready.getId()), ErrorCode.PERIOD_NOT_STARTED);
		expectError(() -> transferService.relieve(minji.getId(), ready.getId()), ErrorCode.PERIOD_NOT_STARTED);
		expectError(() -> transferService.request(suah.getId(), ready.getId(),
				new TransferRequestCreateRequest(minji.getId())), ErrorCode.PERIOD_NOT_STARTED);
	}

	// ---- #21 짐 올리기 부탁 ----

	@Test
	void 짐당_PENDING_부탁은_하나만_만들_수_있다() {
		TransferStatusResponse response = transferService.request(suah.getId(), assignment.getId(),
				new TransferRequestCreateRequest(minji.getId()));
		assertThat(response.status()).isEqualTo(TransferStatus.PENDING);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(minji.getId())).isEmpty();

		expectError(() -> transferService.request(suah.getId(), assignment.getId(),
				new TransferRequestCreateRequest(minji.getId())), ErrorCode.ASSIGNMENT_REQUEST_PENDING);
	}

	@Test
	void 부탁은_담당자만_같은_집의_다른_구성원에게_할_수_있다() {
		expectError(() -> transferService.request(minji.getId(), assignment.getId(),
				new TransferRequestCreateRequest(suah.getId())), ErrorCode.ASSIGNMENT_NOT_MINE);
		expectError(() -> transferService.request(suah.getId(), assignment.getId(),
				new TransferRequestCreateRequest(suah.getId())), ErrorCode.INVALID_INPUT);
		expectError(() -> transferService.request(suah.getId(), assignment.getId(),
				new TransferRequestCreateRequest(outsider.getId())), ErrorCode.INVALID_INPUT);
	}

	// ---- #22 / #23 부탁 받기·거절 ----

	@Test
	void 부탁을_받으면_ACCEPTED_가_되고_담당자가_바뀌며_부탁한_사람에게_알림이_간다() {
		Long transferId = transferService
				.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()))
				.transferId();

		expectError(() -> transferService.accept(suah.getId(), transferId), ErrorCode.TRANSFER_NOT_MINE);
		assertThat(transferService.accept(minji.getId(), transferId).status()).isEqualTo(TransferStatus.ACCEPTED);

		assertThat(assignment.getAssignee()).isEqualTo(minji);
		Notification notification = notificationRepository
				.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(suah.getId()).get(0);
		assertThat(notification.getType()).isEqualTo(NotificationType.REQUEST_ACCEPTED);
		assertThat(notification.getMessage()).isEqualTo("민지가 분리수거를 받았어요");
		expectError(() -> transferService.accept(minji.getId(), transferId), ErrorCode.TRANSFER_ALREADY_HANDLED);
	}

	@Test
	void 부탁을_거절하면_DECLINED_가_되고_짐은_그대로이며_알림이_간다() {
		Long transferId = transferService
				.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()))
				.transferId();

		assertThat(transferService.decline(minji.getId(), transferId).status()).isEqualTo(TransferStatus.DECLINED);

		assertThat(assignment.getAssignee()).isEqualTo(suah);
		Notification notification = notificationRepository
				.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(suah.getId()).get(0);
		assertThat(notification.getType()).isEqualTo(NotificationType.REQUEST_DECLINED);
		assertThat(notification.getMessage()).isEqualTo("민지가 분리수거는 다음에 받는대요");
		expectError(() -> transferService.decline(minji.getId(), transferId), ErrorCode.TRANSFER_ALREADY_HANDLED);
	}

	// ---- #24 취소·되돌리기 ----

	@Test
	void 요청자는_PENDING_부탁을_취소하고_덜어간_사람은_되돌릴_수_있다() {
		Long requestId = transferService
				.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()))
				.transferId();
		expectError(() -> transferService.cancel(minji.getId(), requestId), ErrorCode.TRANSFER_NOT_MINE);
		transferService.cancel(suah.getId(), requestId);
		assertThat(transferRepository.findById(requestId).orElseThrow().getStatus())
				.isEqualTo(TransferStatus.CANCELED);
		expectError(() -> transferService.cancel(suah.getId(), requestId), ErrorCode.TRANSFER_ALREADY_HANDLED);

		Long relieveId = transferService.relieve(minji.getId(), assignment.getId()).transferId();
		expectError(() -> transferService.cancel(suah.getId(), relieveId), ErrorCode.TRANSFER_NOT_MINE);
		transferService.cancel(minji.getId(), relieveId);
		assertThat(assignment.getAssignee()).isEqualTo(suah);
		// 되돌린 덜어가기의 "덜어갔어요" 알림은 원래 담당자에게 안 읽음으로 남지 않는다
		assertThat(notificationRepository.findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(suah.getId(),
				relieveId, NotificationType.CHORE_RELIEVED)).isEmpty();
	}

	// ---- #25 하트 ----

	@Test
	void 하트는_원래_담당자만_한_번_보내며_덜어감_알림이_읽음_처리된다() {
		Long transferId = transferService.relieve(minji.getId(), assignment.getId()).transferId();

		expectError(() -> transferService.heart(minji.getId(), transferId), ErrorCode.TRANSFER_NOT_MINE);

		TransferHeartResponse response = transferService.heart(suah.getId(), transferId);
		assertThat(response.heartedAt()).isNotNull();

		Notification heart = notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(minji.getId())
				.get(0);
		assertThat(heart.getType()).isEqualTo(NotificationType.HEART_RECEIVED);
		assertThat(heart.getMessage()).isEqualTo("수아가 하트를 보냈어요");
		assertThat(notificationRepository.findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(suah.getId(),
				transferId, NotificationType.CHORE_RELIEVED)).isEmpty();
		expectError(() -> transferService.heart(suah.getId(), transferId), ErrorCode.TRANSFER_ALREADY_HANDLED);
	}

	@Test
	void 하트는_덜어가기_이동에만_보낼_수_있다() {
		Long requestId = transferService
				.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()))
				.transferId();
		expectError(() -> transferService.heart(suah.getId(), requestId), ErrorCode.TRANSFER_ALREADY_HANDLED);
	}

	@Test
	void 하트를_받은_덜어가기를_되돌리면_받은_하트_수에서_빠진다() {
		Long transferId = transferService.relieve(minji.getId(), assignment.getId()).transferId();
		transferService.heart(suah.getId(), transferId);
		assertThat(transferRepository.countByToUserIdAndStatusAndHeartedAtIsNotNull(minji.getId(),
				TransferStatus.ACCEPTED)).isEqualTo(1);

		transferService.cancel(minji.getId(), transferId);

		assertThat(transferRepository.countByToUserIdAndStatusAndHeartedAtIsNotNull(minji.getId(),
				TransferStatus.ACCEPTED)).isZero();
	}

	// ---- 개인 한도(5개) ----

	private void giveTodoAssignments(User user, int count) {
		for (int i = 1; i <= count; i++) {
			Chore extra = choreRepository.save(new Chore(house, "추가" + user.getId() + "-" + i));
			assignmentRepository.save(new Assignment(period, extra, user, false));
		}
	}

	@Test
	void 내_짐이_5개면_덜어갈_수_없다() {
		giveTodoAssignments(minji, 5);

		expectError(() -> transferService.relieve(minji.getId(), assignment.getId()),
				ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		assertThat(assignment.getAssignee()).isEqualTo(suah);

		// 완료하면 한도에서 빠진다
		assignmentRepository
				.findAllByPeriodIdAndAssigneeIdAndStatusOrderByIdAsc(period.getId(), minji.getId(),
						AssignmentStatus.TODO)
				.get(0)
				.complete(Instant.now());
		transferService.relieve(minji.getId(), assignment.getId());
		assertThat(assignment.getAssignee()).isEqualTo(minji);
	}

	@Test
	void 내_짐이_5개면_부탁을_받을_수_없다() {
		Long requestId = transferService
				.request(suah.getId(), assignment.getId(), new TransferRequestCreateRequest(minji.getId()))
				.transferId();
		giveTodoAssignments(minji, 5);

		expectError(() -> transferService.accept(minji.getId(), requestId), ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		assertThat(transferRepository.findById(requestId).orElseThrow().getStatus())
				.isEqualTo(TransferStatus.PENDING);
		assertThat(assignment.getAssignee()).isEqualTo(suah);
	}

	@Test
	void 덜어가기를_되돌릴_때_원래_담당자의_짐이_5개면_되돌릴_수_없다() {
		Long relieveId = transferService.relieve(minji.getId(), assignment.getId()).transferId();
		giveTodoAssignments(suah, 5);

		expectError(() -> transferService.cancel(minji.getId(), relieveId), ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		assertThat(assignment.getAssignee()).isEqualTo(minji);
		assertThat(transferRepository.findById(relieveId).orElseThrow().getStatus())
				.isEqualTo(TransferStatus.ACCEPTED);
	}

	@Test
	void 집안일이_삭제된_짐은_되돌릴_수_없고_완료도_ASSIGNMENT_404_이다() {
		Long relieveId = transferService.relieve(minji.getId(), assignment.getId()).transferId();
		assignment.delete(Instant.now());

		expectError(() -> transferService.cancel(minji.getId(), relieveId), ErrorCode.TRANSFER_ALREADY_HANDLED);
		expectError(() -> assignmentService.complete(minji.getId(), assignment.getId()),
				ErrorCode.ASSIGNMENT_NOT_FOUND);
	}

}
