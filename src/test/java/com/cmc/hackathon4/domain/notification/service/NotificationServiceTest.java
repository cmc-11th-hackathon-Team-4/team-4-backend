package com.cmc.hackathon4.domain.notification.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
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

@SpringBootTest
@Transactional
class NotificationServiceTest {

	@Autowired
	NotificationService notificationService;
	@Autowired
	NotificationRepository notificationRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	ChoreRepository choreRepository;
	@Autowired
	AssignmentRepository assignmentRepository;
	@Autowired
	TransferRepository transferRepository;

	User me;
	User other;

	@BeforeEach
	void setUp() {
		me = userRepository.save(new User("noti-device-1", "수아", CharacterType.CHARACTER_1));
		other = userRepository.save(new User("noti-device-2", "민지", CharacterType.CHARACTER_2));
	}

	@Test
	void 받는_사람이_읽음_처리하면_readAt이_기록되고_두_번_해도_성공하며_처음_시각을_유지한다() {
		Notification notification = notificationService.send(me, other, null, NotificationType.PERIOD_ENDED, "결산이 도착했어요");

		notificationService.markRead(me.getId(), notification.getId());
		var firstReadAt = notification.getReadAt();
		notificationService.markRead(me.getId(), notification.getId());

		assertThat(firstReadAt).isNotNull();
		assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
	}

	@Test
	void 남의_알림을_읽음_처리하면_NOTIFICATION_403이고_없는_알림은_404다() {
		Notification notification = notificationService.send(me, null, null, NotificationType.PERIOD_STARTED, "새 집안일이 배분됐어요");

		assertThatThrownBy(() -> notificationService.markRead(other.getId(), notification.getId()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_MINE));
		assertThat(notification.getReadAt()).isNull();
		assertThatThrownBy(() -> notificationService.markRead(me.getId(), 999_999L))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
	}

	@Test
	void 목록은_unreadOnly면_안_읽은_것만_최신순으로_주고_afterId보다_큰_것만_준다() {
		Notification n1 = notificationService.send(me, null, null, NotificationType.PERIOD_STARTED, "1");
		Notification n2 = notificationService.send(me, null, null, NotificationType.PERIOD_EXTENDED, "2");
		Notification n3 = notificationService.send(me, other, null, NotificationType.MOOD_NO_CHOICE, "3");
		notificationService.send(other, null, null, NotificationType.PERIOD_ENDED, "남의 알림");
		notificationService.markRead(me.getId(), n2.getId());

		List<NotificationResponse> unread = notificationService.findMine(me.getId(), true, null);
		List<NotificationResponse> all = notificationService.findMine(me.getId(), false, null);
		List<NotificationResponse> after = notificationService.findMine(me.getId(), false, n1.getId());

		assertThat(unread).extracting(NotificationResponse::id).containsExactly(n3.getId(), n1.getId());
		assertThat(all).extracting(NotificationResponse::id).containsExactly(n3.getId(), n2.getId(), n1.getId());
		assertThat(after).extracting(NotificationResponse::id).containsExactly(n3.getId(), n2.getId());
		assertThat(all.get(0).actor().name()).isEqualTo("민지");
		assertThat(all.get(1).readAt()).isNotNull();
	}

	@Test
	void 목록은_최대_50개까지만_준다() {
		for (int i = 0; i < 55; i++) {
			notificationService.send(me, null, null, NotificationType.PERIOD_STARTED, "알림 " + i);
		}

		assertThat(notificationService.findMine(me.getId(), true, null)).hasSize(50);
	}

	@Test
	void 놓친_알림은_lastEventId보다_큰_내_알림을_오래된_순으로_준다() {
		Notification n1 = notificationService.send(me, null, null, NotificationType.PERIOD_STARTED, "1");
		Notification n2 = notificationService.send(me, null, null, NotificationType.PERIOD_EXTENDED, "2");
		Notification n3 = notificationService.send(me, null, null, NotificationType.PERIOD_ENDED, "3");
		notificationService.send(other, null, null, NotificationType.PERIOD_ENDED, "남의 알림");
		notificationService.markRead(me.getId(), n2.getId());

		assertThat(notificationService.findMissed(me.getId(), n1.getId())).extracting(NotificationResponse::id)
				.containsExactly(n2.getId(), n3.getId());
	}

	@Test
	void 짐_이동과_연결된_알림은_choreName_을_담고_그_외에는_null이다() {
		House house = houseRepository.save(new House("302호 우리집", "NOTI-1"));
		Period period = periodRepository.save(new Period(house, 1, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29)));
		period.start();
		Chore chore = choreRepository.save(new Chore(house, "설거지"));
		Assignment assignment = assignmentRepository.save(new Assignment(period, chore, me, false));
		Transfer transfer = transferRepository.save(Transfer.relieve(assignment, me, other, Instant.now()));
		notificationService.send(me, other, transfer, NotificationType.CHORE_RELIEVED, "민지가 설거지를 덜어갔어요");
		notificationService.send(me, null, null, NotificationType.PERIOD_STARTED, "새 집안일이 배분됐어요");

		// 최신순이라 시스템 알림(짐 없음)이 먼저 온다
		List<NotificationResponse> all = notificationService.findMine(me.getId(), false, null);

		assertThat(all).extracting(NotificationResponse::choreName).containsExactly(null, "설거지");
		assertThat(all.get(1).transferId()).isEqualTo(transfer.getId());
	}

}
