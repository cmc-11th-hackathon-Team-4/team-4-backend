package com.cmc.hackathon4.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
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

/** 엔티티 8개가 H2 에서 테이블로 만들어지고(예약어 컬럼 포함) 리포지토리 쿼리가 동작하는지 확인한다. */
@SpringBootTest
@Transactional
class SchemaSmokeTest {

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
	NotificationService notificationService;

	@Test
	void 모든_엔티티가_저장되고_리포지토리_쿼리가_동작한다() {
		User owner = userRepository.save(new User("device-1", "수아", CharacterType.CHARACTER_1));
		User member = userRepository.save(new User("device-2", "민지", CharacterType.CHARACTER_3));
		House house = houseRepository.save(new House("302호 우리집", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, owner, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, member, MemberRole.MEMBER));
		Chore chore = choreRepository.save(new Chore(house, "설거지"));
		LocalDate startDate = LocalDate.of(2026, 11, 23);
		Period period = periodRepository
				.save(new Period(house, 1, startDate, SettlementCycle.WEEKLY.endDateFrom(startDate)));
		Assignment assignment = assignmentRepository.save(new Assignment(period, chore, owner, false));
		Transfer transfer = transferRepository.save(Transfer.relieve(assignment, owner, member, Instant.now()));
		notificationService.send(owner, member, transfer, NotificationType.CHORE_RELIEVED, "민지가 설거지를 덜어갔어요");

		assertThat(userRepository.findByDeviceId("device-2")).contains(member);
		assertThat(houseRepository.findByIdForUpdate(house.getId())).contains(house);
		assertThat(houseMemberRepository.findByUserId(member.getId())).isPresent();
		assertThat(houseMemberRepository.findAllByHouseIdOrderByIdAsc(house.getId())).hasSize(2);
		assertThat(houseMemberRepository.countByHouseId(house.getId())).isEqualTo(2);
		assertThat(choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(house.getId())).hasSize(1);
		assertThat(choreRepository.existsByHouseIdAndNameAndActiveTrue(house.getId(), "설거지")).isTrue();
		assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 29));
		assertThat(periodRepository.findByHouseIdAndStatus(house.getId(), PeriodStatus.READY)).contains(period);
		assertThat(periodRepository.findFirstByHouseIdOrderByRoundDesc(house.getId())).contains(period);
		assertThat(periodRepository.findFirstByHouseIdAndStatusNotOrderByRoundDesc(house.getId(), PeriodStatus.READY))
				.isEmpty();
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId())).hasSize(1);
		assertThat(transferRepository.existsByAssignmentIdAndStatus(assignment.getId(), TransferStatus.ACCEPTED))
				.isTrue();
		assertThat(transferRepository.findAllByToUserIdAndStatusOrderByIdAsc(member.getId(), TransferStatus.ACCEPTED))
				.hasSize(1);
		assertThat(transferRepository.findAllByPeriodIdAndStatus(period.getId(), TransferStatus.ACCEPTED)).hasSize(1);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(owner.getId())).hasSize(1);
		assertThat(notificationRepository.findAllByReceiverIdAndTransferIdAndTypeAndReadAtIsNull(owner.getId(),
				transfer.getId(), NotificationType.CHORE_RELIEVED)).hasSize(1);
		assertThat(SettlementCycle.MONTHLY.endDateFrom(startDate)).isEqualTo(LocalDate.of(2026, 12, 22));
	}

}
