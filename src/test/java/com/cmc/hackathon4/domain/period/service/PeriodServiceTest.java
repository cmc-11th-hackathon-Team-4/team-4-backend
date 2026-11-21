package com.cmc.hackathon4.domain.period.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.CarryChoice;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.chore.service.ChoreService;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.dto.CarryOverRequest;
import com.cmc.hackathon4.domain.period.dto.DistributionCreateRequest;
import com.cmc.hackathon4.domain.period.dto.NextPeriodResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.SettlementResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
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
class PeriodServiceTest {

	@Autowired
	PeriodService periodService;
	@Autowired
	ChoreService choreService;
	@Autowired
	DistributionService distributionService;
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

	Long houseId;
	Long ownerId;
	Long member1Id;
	Long member2Id;

	@BeforeEach
	void setUp() {
		User owner = userRepository.save(new User("device-1", "수아", CharacterType.CHARACTER_1));
		User member1 = userRepository.save(new User("device-2", "민지", CharacterType.CHARACTER_2));
		User member2 = userRepository.save(new User("device-3", "지호", CharacterType.CHARACTER_3));
		House house = houseRepository.save(new House("302호 우리집", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, owner, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, member1, MemberRole.MEMBER));
		houseMemberRepository.save(new HouseMember(house, member2, MemberRole.MEMBER));
		for (int i = 1; i <= 7; i++) {
			choreRepository.save(new Chore(house, "집안일" + i));
		}
		houseId = house.getId();
		ownerId = owner.getId();
		member1Id = member1.getId();
		member2Id = member2.getId();
	}

	@Test
	void 기간을_늘리면_결산일이_미뤄지고_방장_제외_전원에게_알림이_간다() {
		Long periodId = startFirstPeriod();
		LocalDate before = periodRepository.findById(periodId).orElseThrow().getEndDate();

		var response = periodService.extend(houseId, ownerId, new PeriodExtendRequest(3));

		assertThat(response.endDate()).isEqualTo(before.plusDays(3));
		assertThat(notificationTypes(ownerId)).doesNotContain(NotificationType.PERIOD_EXTENDED);
		assertThat(notificationTypes(member1Id)).contains(NotificationType.PERIOD_EXTENDED);
		assertThat(notificationTypes(member2Id)).contains(NotificationType.PERIOD_EXTENDED);
	}

	@Test
	void 기간은_1_3_7일만_늘릴_수_있고_에러는_days_필드로_나간다() {
		startFirstPeriod();

		assertThatThrownBy(() -> periodService.extend(houseId, ownerId, new PeriodExtendRequest(2)))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> {
					assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
					assertThat(e.getMessage()).startsWith("days:");
				});
	}

	@Test
	void 방장이_아니면_기간을_늘리거나_종료할_수_없다() {
		startFirstPeriod();

		assertThatThrownBy(() -> periodService.extend(houseId, member1Id, new PeriodExtendRequest(1)))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_OWNER_ONLY);
		assertThatThrownBy(() -> periodService.end(houseId, member1Id))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_OWNER_ONLY);
	}

	@Test
	void 기간을_종료하면_ENDED_이고_PENDING_부탁은_취소되며_전원에게_알림이_간다() {
		Long periodId = startFirstPeriod();
		Assignment assignment = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId).get(0);
		User other = userRepository.findById(assignment.getAssignee().getId().equals(member1Id) ? member2Id : member1Id)
				.orElseThrow();
		Transfer pending = transferRepository.save(Transfer.request(assignment, assignment.getAssignee(), other));

		var response = periodService.end(houseId, ownerId);

		assertThat(response.status()).isEqualTo(PeriodStatus.ENDED);
		assertThat(response.endedAt()).isNotNull();
		assertThat(pending.getStatus()).isEqualTo(TransferStatus.CANCELED);
		assertThat(periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)).isEmpty();
		for (Long userId : List.of(ownerId, member1Id, member2Id)) {
			assertThat(notificationTypes(userId)).contains(NotificationType.PERIOD_ENDED);
		}
	}

	@Test
	void 종료_다음_주기_만들기_시작하기로_상태가_전이된다() {
		Long period1Id = startFirstPeriod();
		periodService.end(houseId, ownerId);

		assertThat(periodService.getNext(houseId, member1Id).prepared()).isFalse();
		assertThatThrownBy(() -> periodService.startNext(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NEXT_NOT_READY);

		NextPeriodResponse next = periodService.createNext(houseId, ownerId);
		Period period1 = periodRepository.findById(period1Id).orElseThrow();
		assertThat(next.prepared()).isTrue();
		assertThat(next.period().status()).isEqualTo(PeriodStatus.READY);
		assertThat(next.period().round()).isEqualTo(2);
		// 일찍 종료했어도 다음 기간은 직전 종료일 다음 날이 아니라 오늘(KST)부터다
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		assertThat(period1.getEndDate()).isAfter(today);
		assertThat(next.period().startDate()).isEqualTo(today);
		assertThat(next.period().endDate()).isEqualTo(SettlementCycle.WEEKLY.endDateFrom(today));
		assertThat(next.totalCount()).isEqualTo(7);
		NextPeriodResponse viewed = periodService.getNext(houseId, member1Id);
		assertThat(viewed.prepared()).isTrue();
		assertThat(viewed.period().id()).isEqualTo(next.period().id());

		var started = periodService.startNext(houseId, ownerId);

		assertThat(started.period().status()).isEqualTo(PeriodStatus.ACTIVE);
		assertThat(started.period().startDate()).isEqualTo(today);
		assertThat(started.period().endDate()).isEqualTo(SettlementCycle.WEEKLY.endDateFrom(today));
		assertThat(started.period().round()).isEqualTo(2);
		assertThat(houseRepository.findById(houseId).orElseThrow().getStatus()).isEqualTo(HouseStatus.ACTIVE);
		assertThat(notificationTypes(member1Id)).contains(NotificationType.PERIOD_STARTED);
		assertThatThrownBy(() -> periodService.startNext(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
		assertThatThrownBy(() -> periodService.getNext(houseId, member1Id))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
		CarryOverRequest late = new CarryOverRequest(List.of(new CarryOverRequest.Choice(1L, CarryChoice.KEEP)));
		assertThatThrownBy(() -> periodService.saveCarryOvers(period1Id, ownerId, late))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
	}

	@Test
	void 이월된_짐은_KEEP_과_미선택이_같은_담당자에게_고정되고_RELEASE_는_다시_섞인다() {
		Long period1Id = startFirstPeriod();
		List<Assignment> all = assignmentRepository.findAllByPeriodIdOrderByIdAsc(period1Id);
		List<Assignment> ownerOnes = all.stream().filter(a -> a.getAssignee().getId().equals(ownerId)).toList();
		all.stream().filter(a -> !ownerOnes.contains(a)).forEach(a -> a.complete(Instant.now()));
		Long releasedId = ownerOnes.get(0).getId();
		Long releasedChoreId = ownerOnes.get(0).getChore().getId();
		Long keptId = ownerOnes.get(1).getId();
		// 첫 번째는 RELEASE, 두 번째는 KEEP, 나머지(있다면)는 선택하지 않는다.
		Set<Long> expectedCarried = ownerOnes.stream()
				.skip(1)
				.map(a -> a.getChore().getId())
				.collect(Collectors.toSet());
		periodService.end(houseId, ownerId);
		periodService.saveCarryOvers(period1Id, ownerId, new CarryOverRequest(List.of(
				new CarryOverRequest.Choice(releasedId, CarryChoice.RELEASE),
				new CarryOverRequest.Choice(keptId, CarryChoice.KEEP))));
		assertThat(periodService.getSettlement(period1Id, ownerId).myUndone())
				.filteredOn(choice -> choice.assignmentId().equals(releasedId)).singleElement()
				.extracting(SettlementResponse.MyUndone::carryChoice).isEqualTo(CarryChoice.RELEASE);

		NextPeriodResponse next = periodService.createNext(houseId, ownerId);

		NextPeriodResponse.Member owner = next.members().stream().filter(m -> m.userId().equals(ownerId)).findFirst()
				.orElseThrow();
		assertThat(choreIds(owner, true)).isEqualTo(expectedCarried);
		assertThat(owner.chores().subList(0, expectedCarried.size()))
				.allMatch(NextPeriodResponse.Chore::carriedOver);
		next.members().stream().filter(m -> !m.userId().equals(ownerId)).forEach(m -> assertThat(choreIds(m, true)).isEmpty());
		assertThat(next.members().stream().flatMap(m -> m.chores().stream()).filter(c -> !c.carriedOver())
				.map(NextPeriodResponse.Chore::choreId)).contains(releasedChoreId);
		assertThat(next.totalCount()).isEqualTo(7);
		List<Integer> counts = next.members().stream().map(m -> m.chores().size()).toList();
		assertThat(counts.stream().max(Integer::compare).orElseThrow() - counts.stream().min(Integer::compare).orElseThrow())
				.isLessThanOrEqualTo(1);

		// 다시 나눠도 이월 칸은 그대로다.
		NextPeriodResponse again = periodService.createNext(houseId, ownerId);
		assertThat(again.period().id()).isEqualTo(next.period().id());
		NextPeriodResponse.Member ownerAgain = again.members().stream().filter(m -> m.userId().equals(ownerId))
				.findFirst().orElseThrow();
		assertThat(choreIds(ownerAgain, true)).isEqualTo(expectedCarried);
		assertThat(again.totalCount()).isEqualTo(7);

		// READY 인 다음 기간이 있을 때 이월 선택을 바꾸면 그 배분이 다시 만들어진다.
		periodService.saveCarryOvers(period1Id, ownerId,
				new CarryOverRequest(List.of(new CarryOverRequest.Choice(releasedId, CarryChoice.KEEP))));
		NextPeriodResponse changed = periodService.getNext(houseId, member1Id);
		NextPeriodResponse.Member ownerChanged = changed.members().stream().filter(m -> m.userId().equals(ownerId))
				.findFirst().orElseThrow();
		assertThat(choreIds(ownerChanged, true)).hasSize(ownerOnes.size()).contains(releasedChoreId);
		assertThat(changed.totalCount()).isEqualTo(7);
	}

	@Test
	void 남의_짐이나_완료된_짐은_이월_선택할_수_없다() {
		Long periodId = startFirstPeriod();
		List<Assignment> all = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId);
		Assignment notMine = all.stream().filter(a -> !a.getAssignee().getId().equals(ownerId)).findFirst().orElseThrow();
		Assignment mine = all.stream().filter(a -> a.getAssignee().getId().equals(ownerId)).findFirst().orElseThrow();
		mine.complete(Instant.now());
		periodService.end(houseId, ownerId);

		CarryOverRequest request = new CarryOverRequest(
				List.of(new CarryOverRequest.Choice(notMine.getId(), CarryChoice.RELEASE)));
		assertThatThrownBy(() -> periodService.saveCarryOvers(periodId, ownerId, request))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.ASSIGNMENT_NOT_MINE);
		CarryOverRequest completed = new CarryOverRequest(
				List.of(new CarryOverRequest.Choice(mine.getId(), CarryChoice.RELEASE)));
		assertThatThrownBy(() -> periodService.saveCarryOvers(periodId, ownerId, completed))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ASSIGNMENT_NOT_MINE));
	}

	@Test
	void 기간이_끝나기_전에는_이월_선택을_저장할_수_없다() {
		Long periodId = startFirstPeriod();
		Assignment mine = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId).stream()
				.filter(a -> a.getAssignee().getId().equals(ownerId)).findFirst().orElseThrow();
		CarryOverRequest request = new CarryOverRequest(
				List.of(new CarryOverRequest.Choice(mine.getId(), CarryChoice.RELEASE)));

		assertThatThrownBy(() -> periodService.saveCarryOvers(periodId, ownerId, request))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PERIOD_NOT_ENDED));
		assertThat(mine.getCarryChoice()).isNull();
	}

	@Test
	void 기간_종료_뒤_참여한_구성원도_다음_주기_배분에_포함된다() {
		Long periodId = startFirstPeriod();
		// 못 한 일이 없어야 이월 없이 전부 새로 나뉜다.
		assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId).forEach(a -> a.complete(Instant.now()));
		periodService.end(houseId, ownerId);
		User newcomer = userRepository.save(new User("device-4", "하늘", CharacterType.CHARACTER_4));
		houseMemberRepository.save(new HouseMember(houseRepository.findById(houseId).orElseThrow(), newcomer,
				MemberRole.MEMBER));

		NextPeriodResponse next = periodService.createNext(houseId, ownerId);

		assertThat(next.members()).hasSize(4);
		assertThat(next.members().stream().map(m -> m.chores().size()).toList()).containsExactlyInAnyOrder(2, 2, 2, 1);
	}

	@Test
	void 배려왕이_동점이면_공동이고_결산에_도움_횟수와_하트가_집계된다() {
		Long periodId = startFirstPeriod();
		List<Assignment> all = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId);
		Assignment ownersOne = all.stream().filter(a -> a.getAssignee().getId().equals(ownerId)).findFirst().orElseThrow();
		Assignment member1sOne = all.stream().filter(a -> a.getAssignee().getId().equals(member1Id)).findFirst()
				.orElseThrow();
		User member1 = userRepository.findById(member1Id).orElseThrow();
		User member2 = userRepository.findById(member2Id).orElseThrow();
		// member1 이 방장의 짐을, member2 가 member1 의 짐을 덜어간다 → 1 대 1 동점
		Transfer first = transferRepository
				.save(Transfer.relieve(ownersOne, ownersOne.getAssignee(), member1, Instant.now()));
		ownersOne.changeAssignee(member1);
		transferRepository.save(Transfer.relieve(member1sOne, member1, member2, Instant.now()));
		member1sOne.changeAssignee(member2);
		first.heart(Instant.now());
		all.stream().filter(a -> a != ownersOne && a != member1sOne).forEach(a -> a.complete(Instant.now()));
		periodService.end(houseId, ownerId);

		SettlementResponse settlement = periodService.getSettlement(periodId, member1Id);

		assertThat(settlement.summary().totalCount()).isEqualTo(7);
		assertThat(settlement.summary().doneCount()).isEqualTo(5);
		assertThat(settlement.summary().undoneCount()).isEqualTo(2);
		assertThat(settlement.summary().helpedCount()).isEqualTo(2);
		assertThat(settlement.kindness().kingUserIds()).containsExactlyInAnyOrder(member1Id, member2Id);
		assertThat(settlement.kindness().ranking().get(0).helpedCount()).isEqualTo(1);
		assertThat(settlement.kindness().ranking().stream().filter(r -> r.userId().equals(member1Id)).findFirst()
				.orElseThrow().receivedHeartCount()).isEqualTo(1);
		assertThat(settlement.chores().stream().filter(c -> c.assignmentId().equals(ownersOne.getId())).findFirst()
				.orElseThrow().helpedBy().userId()).isEqualTo(member1Id);
		assertThat(settlement.myUndone()).extracting(SettlementResponse.MyUndone::assignmentId)
				.containsExactly(ownersOne.getId());
		assertThat(settlement.myUndone().get(0).carryChoice()).isEqualTo(CarryChoice.KEEP);
		assertThat(settlement.nextPeriodStarted()).isFalse();
	}

	@Test
	void 배려왕은_도움이_한_번도_없으면_빈_배열이다() {
		Long periodId = startFirstPeriod();
		periodService.end(houseId, ownerId);

		assertThat(periodService.getSettlement(periodId, ownerId).kindness().kingUserIds()).isEmpty();
	}

	@Test
	void 삭제된_집안일에_대한_도움은_결산에서_세지_않는다() {
		Long periodId = startFirstPeriod();
		Assignment ownersOne = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId)
				.stream()
				.filter(a -> a.getAssignee().getId().equals(ownerId))
				.findFirst()
				.orElseThrow();
		User member1 = userRepository.findById(member1Id).orElseThrow();
		transferRepository.save(Transfer.relieve(ownersOne, ownersOne.getAssignee(), member1, Instant.now()));
		ownersOne.changeAssignee(member1);
		ownersOne.delete(Instant.now());
		periodService.end(houseId, ownerId);

		SettlementResponse settlement = periodService.getSettlement(periodId, ownerId);

		assertThat(settlement.summary().helpedCount()).isZero();
		assertThat(settlement.kindness().kingUserIds()).isEmpty();
	}

	@Test
	void 구성원이_아니면_결산을_볼_수_없다() {
		Long periodId = startFirstPeriod();
		periodService.end(houseId, ownerId);
		User stranger = userRepository.save(new User("device-9", "낯선이", CharacterType.CHARACTER_4));

		assertThatThrownBy(() -> periodService.getSettlement(periodId, stranger.getId()))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_NOT_MEMBER);
	}

	@Test
	void 진행_중인_기간은_다음_주기를_만들_수_없고_결산도_볼_수_없다() {
		Long periodId = startFirstPeriod();

		assertThatThrownBy(() -> periodService.createNext(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NOT_ENDED);
		assertThatThrownBy(() -> periodService.getSettlement(periodId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NOT_ENDED);
		assertThatThrownBy(() -> periodService.startNext(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
	}

	@Test
	void 시작한_뒤_추가한_집안일은_기간이_끝나면_비활성화되고_다음_배분에서_빠진다() {
		Long periodId = startFirstPeriod();
		Long addedChoreId = choreService.createChore(houseId, member1Id, new ChoreCreateRequest("임시 집안일")).chore().id();

		periodService.end(houseId, ownerId);

		assertThat(choreRepository.findById(addedChoreId).orElseThrow().isActive()).isFalse();
		// 결산에는 그대로 보이지만 내 못 한 일(이월 선택 대상)에서는 빠진다
		SettlementResponse settlement = periodService.getSettlement(periodId, member1Id);
		assertThat(settlement.chores()).extracting(SettlementResponse.Chore::name).contains("임시 집안일");
		assertThat(settlement.summary().totalCount()).isEqualTo(8);
		assertThat(settlement.myUndone()).extracting(SettlementResponse.MyUndone::name).doesNotContain("임시 집안일");

		NextPeriodResponse next = periodService.createNext(houseId, ownerId);
		assertThat(next.totalCount()).isEqualTo(7);
		assertThat(next.members().stream().flatMap(member -> member.chores().stream()))
				.extracting(NextPeriodResponse.Chore::choreId)
				.doesNotContain(addedChoreId);
	}

	// Regression: ISSUE-004 — 다음 기간이 준비된 뒤 추가한 집안일이 다음 회차 배분에서 빠지던 문제
	// Found by /qa on 2026-11-22
	@Test
	void 다음_기간이_준비된_뒤_추가한_집안일도_다음_기간_배분에_들어간다() {
		startFirstPeriod();
		periodService.end(houseId, ownerId);
		Long nextPeriodId = periodService.createNext(houseId, ownerId).period().id();

		Long choreId = choreService.createChore(houseId, member1Id, new ChoreCreateRequest("화분 물주기")).chore().id();

		List<Assignment> assignments = assignmentRepository.findAllByPeriodIdOrderByIdAsc(nextPeriodId);
		assertThat(assignments).hasSize(8);
		assertThat(assignments).extracting(a -> a.getChore().getId()).containsOnlyOnce(choreId);
	}

	// Regression: ISSUE-005 — 늦게 시작한 다음 기간이 이미 지난 날짜로 시작되던 문제
	// Found by /qa on 2026-11-22
	@Test
	void 준비해_둔_다음_기간을_며칠_뒤에_시작해도_시작하는_날부터다() {
		startFirstPeriod();
		periodService.end(houseId, ownerId);
		Long nextPeriodId = periodService.createNext(houseId, ownerId).period().id();
		// 방장이 다음 기간을 만들어 두고 열흘 뒤에 시작한 상황
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		periodRepository.findById(nextPeriodId)
				.orElseThrow()
				.reschedule(today.minusDays(10), SettlementCycle.WEEKLY.endDateFrom(today.minusDays(10)));

		var started = periodService.startNext(houseId, ownerId);

		assertThat(started.period().startDate()).isEqualTo(today);
		assertThat(started.period().endDate()).isEqualTo(SettlementCycle.WEEKLY.endDateFrom(today));
	}

	@Test
	void 기간_사이에_추가한_집안일은_임시로_다음_기간에_배분되고_그_기간이_끝나면_비활성화된다() {
		startFirstPeriod();
		periodService.end(houseId, ownerId);
		Long nextPeriodId = periodService.createNext(houseId, ownerId).period().id();

		Long choreId = choreService.createChore(houseId, member1Id, new ChoreCreateRequest("화분 물주기")).chore().id();

		Chore added = choreRepository.findById(choreId).orElseThrow();
		assertThat(added.isTemporary()).isTrue();
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(nextPeriodId))
				.extracting(a -> a.getChore().getId())
				.containsOnlyOnce(choreId);

		periodService.startNext(houseId, ownerId);
		periodService.end(houseId, ownerId);

		assertThat(choreRepository.findById(choreId).orElseThrow().isActive()).isFalse();
	}

	@Test
	void 기간_사이에도_활성_집안일이_10개면_추가할_수_없다() {
		startFirstPeriod();
		periodService.end(houseId, ownerId);
		long active = choreRepository.countByHouseIdAndActiveTrue(houseId);
		for (long i = active; i < 10; i++) {
			choreService.createChore(houseId, ownerId, new ChoreCreateRequest("추가" + i));
		}

		assertThatThrownBy(() -> choreService.createChore(houseId, ownerId, new ChoreCreateRequest("초과")))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.CHORE_LIMIT_EXCEEDED);
	}

	private Long startFirstPeriod() {
		distributionService.create(houseId, ownerId,
				new DistributionCreateRequest(SettlementCycle.WEEKLY, LocalDate.now(ZoneId.of("Asia/Seoul"))));
		distributionService.start(houseId, ownerId);
		return periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).orElseThrow().getId();
	}

	private List<NotificationType> notificationTypes(Long userId) {
		return notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(userId)
				.stream()
				.map(Notification::getType)
				.toList();
	}

	private Set<Long> choreIds(NextPeriodResponse.Member member, boolean carriedOver) {
		return member.chores()
				.stream()
				.filter(chore -> chore.carriedOver() == carriedOver)
				.map(NextPeriodResponse.Chore::choreId)
				.collect(Collectors.toSet());
	}

}
