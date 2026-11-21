package com.cmc.hackathon4.domain.period.service;

import java.time.Instant;
import java.time.LocalDate;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PeriodAutoEndSchedulerTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 23);
	private static final LocalDate END = LocalDate.of(2026, 11, 29);

	@Autowired
	PeriodAutoEndScheduler scheduler;
	@Autowired
	PeriodService periodService;
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

	@Test
	void 결산일이_지난_기간은_종료되고_PENDING_부탁은_취소되며_전원에게_알림이_간다() {
		User owner = userRepository.save(new User("auto-end-1", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("auto-end-2", "민지", CharacterType.CHARACTER_3));
		Period period = activePeriod(owner, member);
		Chore chore = choreRepository.save(new Chore(period.getHouse(), "설거지"));
		Assignment assignment = assignmentRepository.save(new Assignment(period, chore, owner, false));
		Transfer pending = transferRepository.save(Transfer.request(assignment, owner, member));

		int ended = scheduler.endExpiredPeriods(END.plusDays(1));

		assertThat(ended).isEqualTo(1);
		assertThat(period.getStatus()).isEqualTo(PeriodStatus.ENDED);
		assertThat(period.getEndedAt()).isNotNull();
		assertThat(pending.getStatus()).isEqualTo(TransferStatus.CANCELED);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(owner.getId()))
				.extracting(Notification::getType)
				.containsExactly(NotificationType.PERIOD_ENDED);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(member.getId()))
				.extracting(Notification::getType)
				.containsExactly(NotificationType.PERIOD_ENDED);
	}

	@Test
	void 결산일_당일에는_종료하지_않는다() {
		User owner = userRepository.save(new User("auto-end-3", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("auto-end-4", "민지", CharacterType.CHARACTER_3));
		Period period = activePeriod(owner, member);

		int ended = scheduler.endExpiredPeriods(END);

		assertThat(ended).isZero();
		assertThat(period.getStatus()).isEqualTo(PeriodStatus.ACTIVE);
	}

	@Test
	void 이미_종료된_기간과_시작_전_기간은_건드리지_않는다() {
		User owner = userRepository.save(new User("auto-end-5", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("auto-end-6", "민지", CharacterType.CHARACTER_3));
		Period ended = activePeriod(owner, member);
		scheduler.endExpiredPeriods(END.plusDays(1));
		Period ready = periodRepository.save(new Period(ended.getHouse(), 2, END.plusDays(1),
				END.plusDays(7)));

		int endedAgain = scheduler.endExpiredPeriods(END.plusDays(30));

		assertThat(endedAgain).isZero();
		assertThat(ready.getStatus()).isEqualTo(PeriodStatus.READY);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(owner.getId())).hasSize(1);
	}

	@Test
	void 자동_종료_대상을_조회한_뒤_기간을_연장하면_종료하지_않는다() {
		User owner = userRepository.save(new User("auto-end-7", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("auto-end-8", "민지", CharacterType.CHARACTER_3));
		Period period = activePeriod(owner, member);
		LocalDate today = END.plusDays(1);
		assertThat(periodService.findExpiredPeriodIds(today)).contains(period.getId());
		period.extend(3);

		assertThat(periodService.endIfExpired(period.getId(), today)).isFalse();
		assertThat(period.getStatus()).isEqualTo(PeriodStatus.ACTIVE);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(owner.getId())).isEmpty();
	}

	@Test
	void 자동_종료_대상을_조회한_뒤_이미_종료됐거나_없어진_기간은_건너뛴다() {
		User owner = userRepository.save(new User("auto-end-9", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("auto-end-10", "민지", CharacterType.CHARACTER_3));
		Period period = activePeriod(owner, member);
		LocalDate today = END.plusDays(1);
		assertThat(periodService.findExpiredPeriodIds(today)).contains(period.getId());
		Instant endedAt = Instant.parse("2026-11-29T15:00:00Z");
		period.end(endedAt);

		assertThat(periodService.endIfExpired(period.getId(), today)).isFalse();
		assertThat(period.getEndedAt()).isEqualTo(endedAt);
		assertThat(periodService.endIfExpired(Long.MAX_VALUE, today)).isFalse();
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(owner.getId())).isEmpty();
	}

	private Period activePeriod(User owner, User member) {
		House house = houseRepository.save(new House("302호 우리집", "AE" + owner.getId() % 10000));
		house.startDistributing(7);
		house.activate();
		houseMemberRepository.save(new HouseMember(house, owner, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, member, MemberRole.MEMBER));
		Period period = periodRepository.save(new Period(house, 1, START, END));
		period.start();
		return period;
	}

}
