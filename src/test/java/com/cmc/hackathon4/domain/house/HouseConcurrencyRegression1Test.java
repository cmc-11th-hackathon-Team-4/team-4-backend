package com.cmc.hackathon4.domain.house;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.house.service.HouseService;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.period.service.DistributionService;
import com.cmc.hackathon4.domain.transfer.dto.TransferRequestCreateRequest;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.transfer.service.TransferService;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Regression: ISSUE-001~004 — 같은 집에 동시에 들어온 쓰기 요청이 직렬화되지 않아 둘 다 성공하던 문제
// Found by /qa on 2026-11-22
// Report: .gstack/qa-reports/run-20261121T173501Z/qa-report-team-4-backend-api-2026-11-22.md
/**
 * 같은 집에 동시에 들어온 요청은 하나씩 처리되어야 한다. 실제 커밋이 필요해 트랜잭션 롤백을 쓰지 않고, 끝나면 만든 데이터를 직접 지운다.
 */
@SpringBootTest
class HouseConcurrencyRegression1Test {

	private static final int ROUNDS = 15;
	private static final AtomicInteger SEQUENCE = new AtomicInteger();

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
	HouseService houseService;
	@Autowired
	DistributionService distributionService;
	@Autowired
	TransferService transferService;

	@AfterEach
	void cleanUp() {
		notificationRepository.deleteAllInBatch();
		transferRepository.deleteAllInBatch();
		assignmentRepository.deleteAllInBatch();
		periodRepository.deleteAllInBatch();
		choreRepository.deleteAllInBatch();
		houseMemberRepository.deleteAllInBatch();
		houseRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	void 두_사람이_같은_짐을_동시에_덜어가도_이동_기록과_담당자가_어긋나지_않는다() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			Fixture f = startedHouse(3);
			Assignment assignment = assignmentRepository.save(new Assignment(f.period, f.chore, f.users.get(0), false));

			runTogether(() -> transferService.relieve(f.users.get(1).getId(), assignment.getId()),
					() -> transferService.relieve(f.users.get(2).getId(), assignment.getId()));

			// 차례로 처리되면 뒤 사람은 앞 사람에게서 덜어간다. 같은 사람에게서 두 번 덜어간 기록이 남으면 안 된다.
			List<Transfer> transfers = transferRepository.findAllByAssignmentIdAndStatus(assignment.getId(),
					TransferStatus.ACCEPTED);
			assertThat(transfers).extracting(t -> t.getFromUser().getId()).doesNotHaveDuplicates();
			Long lastTaker = transfers.stream()
					.max(Comparator.comparing(Transfer::getId))
					.orElseThrow()
					.getToUser()
					.getId();
			assertThat(assignmentRepository.findById(assignment.getId()).orElseThrow().getAssignee().getId())
					.isEqualTo(lastTaker);
		}
	}

	@Test
	void 같은_짐을_동시에_두_사람에게_부탁하면_PENDING_은_하나만_남는다() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			Fixture f = startedHouse(3);
			Long ownerId = f.users.get(0).getId();
			Assignment assignment = assignmentRepository.save(new Assignment(f.period, f.chore, f.users.get(0), false));

			int succeeded = runTogether(
					() -> transferService.request(ownerId, assignment.getId(),
							new TransferRequestCreateRequest(f.users.get(1).getId())),
					() -> transferService.request(ownerId, assignment.getId(),
							new TransferRequestCreateRequest(f.users.get(2).getId())));

			assertThat(succeeded).isEqualTo(1);
			assertThat(transferRepository.findAllByAssignmentIdAndStatus(assignment.getId(), TransferStatus.PENDING))
					.hasSize(1);
		}
	}

	@Test
	void 배분_확정을_동시에_두_번_눌러도_한_번만_시작되고_알림도_한_번만_간다() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			Fixture f = distributingHouse(2);
			Long ownerId = f.users.get(0).getId();
			Long memberId = f.users.get(1).getId();

			int succeeded = runTogether(() -> distributionService.start(f.house.getId(), ownerId),
					() -> distributionService.start(f.house.getId(), ownerId));

			assertThat(succeeded).isEqualTo(1);
			assertThat(notificationRepository.findAll()
					.stream()
					.filter(n -> n.getReceiver().getId().equals(memberId) && n.getType() == NotificationType.PERIOD_STARTED))
					.hasSize(1);
		}
	}

	@Test
	void 마지막_한_자리에_동시에_참여해도_정원을_넘지_않는다() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			// 정원에서 한 자리만 남긴다 (정원 값이 바뀌어도 "마지막 한 자리" 를 시험하도록)
			Fixture f = waitingHouse(House.MAX_MEMBERS - 1);
			User first = newUser();
			User second = newUser();
			HouseJoinRequest request = new HouseJoinRequest(f.house.getInviteCode());

			int succeeded = runTogether(() -> houseService.join(first.getId(), request),
					() -> houseService.join(second.getId(), request));

			assertThat(succeeded).isEqualTo(1);
			assertThat(houseMemberRepository.countByHouseId(f.house.getId())).isEqualTo(House.MAX_MEMBERS);
		}
	}

	/** 두 작업을 같은 순간에 시작시키고 성공한 개수를 돌려준다. BusinessException 은 정상적인 거절로 센다. */
	private int runTogether(Callable<?> first, Callable<?> second) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CyclicBarrier barrier = new CyclicBarrier(2);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (Callable<?> task : List.of(first, second)) {
				futures.add(pool.submit(() -> {
					barrier.await(5, TimeUnit.SECONDS);
					try {
						task.call();
						return true;
					}
					catch (BusinessException e) {
						return false;
					}
				}));
			}
			int succeeded = 0;
			for (Future<Boolean> future : futures) {
				if (future.get(20, TimeUnit.SECONDS)) {
					succeeded++;
				}
			}
			return succeeded;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private Fixture waitingHouse(int memberCount) {
		int n = SEQUENCE.incrementAndGet();
		House house = houseRepository.save(new House("동시성 " + n, "C%05d".formatted(n)));
		List<User> users = new ArrayList<>();
		for (int i = 0; i < memberCount; i++) {
			User user = newUser();
			houseMemberRepository.save(new HouseMember(house, user, i == 0 ? MemberRole.OWNER : MemberRole.MEMBER));
			users.add(user);
		}
		Chore chore = choreRepository.save(new Chore(house, "분리수거"));
		return new Fixture(house, users, chore, null);
	}

	private Fixture distributingHouse(int memberCount) {
		Fixture f = waitingHouse(memberCount);
		f.house.startDistributing(SettlementCycle.WEEKLY);
		House house = houseRepository.save(f.house);
		LocalDate start = LocalDate.of(2026, 11, 23);
		Period period = periodRepository.save(new Period(house, 1, start, SettlementCycle.WEEKLY.endDateFrom(start)));
		return new Fixture(house, f.users, f.chore, period);
	}

	private Fixture startedHouse(int memberCount) {
		Fixture f = distributingHouse(memberCount);
		f.house.activate();
		House house = houseRepository.save(f.house);
		f.period.start();
		Period period = periodRepository.save(f.period);
		return new Fixture(house, f.users, f.chore, period);
	}

	private User newUser() {
		int n = SEQUENCE.incrementAndGet();
		return userRepository.save(new User("concurrency-device-" + n, "동시" + n, CharacterType.CHARACTER_1));
	}

	private record Fixture(House house, List<User> users, Chore chore, Period period) {
	}

}
