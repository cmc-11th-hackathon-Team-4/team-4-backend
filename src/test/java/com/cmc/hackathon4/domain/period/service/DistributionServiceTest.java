package com.cmc.hackathon4.domain.period.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.dto.DistributionCreateRequest;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
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
class DistributionServiceTest {

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
	NotificationRepository notificationRepository;
	@Autowired
	com.cmc.hackathon4.domain.house.service.HouseService houseService;

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
	void 배분을_만들면_집은_DISTRIBUTING_이고_개수_차이는_최대_1이다() {
		DistributionResponse response = distributionService.create(houseId, ownerId, request());

		assertThat(houseRepository.findById(houseId).orElseThrow().getStatus()).isEqualTo(HouseStatus.DISTRIBUTING);
		assertThat(response.period().status()).isEqualTo(PeriodStatus.READY);
		assertThat(response.period().round()).isEqualTo(1);
		assertThat(response.totalCount()).isEqualTo(7);
		List<Integer> counts = response.members().stream().map(member -> member.chores().size()).toList();
		assertThat(counts).containsExactlyInAnyOrder(3, 2, 2);
		assertThat(response.members().stream().filter(DistributionResponse.Member::isMe).count()).isEqualTo(1);
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(response.period().id())).hasSize(7);
	}

	@Test
	void 방장이_아니면_배분을_만들_수_없다() {
		assertThatThrownBy(() -> distributionService.create(houseId, member1Id, request()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_OWNER_ONLY);
	}

	@Test
	void 시작일이_과거이면_배분을_만들_수_없다() {
		DistributionCreateRequest past = new DistributionCreateRequest(7,
				LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1));

		assertThatThrownBy(() -> distributionService.create(houseId, ownerId, past))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void 집안일이_없으면_배분을_만들_수_없다() {
		choreRepository.findAllByHouseId(houseId).forEach(Chore::deactivate);

		assertThatThrownBy(() -> distributionService.create(houseId, ownerId, request()))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void 다시_호출하면_같은_기간의_주기와_날짜를_갱신하고_배분을_새로_만든다() {
		Long periodId = distributionService.create(houseId, ownerId, request()).period().id();
		LocalDate newStart = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(5);

		DistributionResponse response = distributionService.create(houseId, ownerId,
				new DistributionCreateRequest(3, newStart));

		assertThat(response.period().id()).isEqualTo(periodId);
		assertThat(response.settlementCycle()).isEqualTo(3);
		assertThat(response.period().startDate()).isEqualTo(newStart);
		assertThat(response.period().endDate()).isEqualTo(newStart.plusDays(2));
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId)).hasSize(7);
	}

	@Test
	void 다시_섞어도_개수_차이는_최대_1이고_조회_결과와_같다() {
		distributionService.create(houseId, ownerId, request());

		DistributionResponse shuffled = distributionService.shuffle(houseId, ownerId);
		DistributionResponse viewed = distributionService.get(houseId, member1Id);

		assertThat(shuffled.members().stream().map(member -> member.chores().size()).toList())
				.containsExactlyInAnyOrder(3, 2, 2);
		assertThat(viewed.members().stream().map(member -> member.chores().stream().map(c -> c.choreId()).toList()))
				.containsExactlyElementsOf(
						shuffled.members().stream().map(member -> member.chores().stream().map(c -> c.choreId()).toList())
								.toList());
		assertThat(viewed.members().stream().filter(DistributionResponse.Member::isMe).findFirst().orElseThrow()
				.userId()).isEqualTo(member1Id);
	}

	@Test
	void 확정하면_기간과_집이_ACTIVE_가_되고_방장_제외_전원에게_알림이_간다() {
		Long periodId = distributionService.create(houseId, ownerId, request()).period().id();

		var response = distributionService.start(houseId, ownerId);

		assertThat(response.status()).isEqualTo(HouseStatus.ACTIVE);
		assertThat(response.period().status()).isEqualTo(PeriodStatus.ACTIVE);
		Period period = periodRepository.findById(periodId).orElseThrow();
		assertThat(period.getStatus()).isEqualTo(PeriodStatus.ACTIVE);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(ownerId)).isEmpty();
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(member1Id)).hasSize(1);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(member2Id)).hasSize(1);
	}

	@Test
	void 시작한_집은_조회_다시_섞기_확정_재생성이_모두_막힌다() {
		distributionService.create(houseId, ownerId, request());
		distributionService.start(houseId, ownerId);

		assertThatThrownBy(() -> distributionService.get(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_NOT_DISTRIBUTING);
		assertThatThrownBy(() -> distributionService.shuffle(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_NOT_DISTRIBUTING);
		assertThatThrownBy(() -> distributionService.start(houseId, ownerId))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_NOT_DISTRIBUTING);
		assertThatThrownBy(() -> distributionService.create(houseId, ownerId, request()))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HOUSE_ALREADY_STARTED);
	}

	@Test
	void 정한_시작일이_지난_뒤_확정하면_오늘부터_기간을_다시_잡는다() {
		Long periodId = distributionService.create(houseId, ownerId, request()).period().id();
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		Period ready = periodRepository.findById(periodId).orElseThrow();
		ready.reschedule(today.minusDays(10), today.minusDays(4));

		distributionService.start(houseId, ownerId);

		Period started = periodRepository.findById(periodId).orElseThrow();
		assertThat(started.getStartDate()).isEqualTo(today);
		assertThat(started.getEndDate()).isEqualTo(today.plusDays(6));
	}

	@Test
	void 배분_확인_중에_주기를_바꾸면_1회차_기간의_종료일도_새_주기로_바뀐다() {
		var created = distributionService.create(houseId, ownerId, request());

		houseService.update(houseId, ownerId,
				new com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest(null, 1));

		Period ready = periodRepository.findById(created.period().id()).orElseThrow();
		assertThat(ready.getEndDate()).isEqualTo(created.period().startDate());
	}

	@Test
	void 결산_주기는_1_3_7일만_고를_수_있고_에러는_settlementCycle_필드로_나간다() {
		DistributionCreateRequest invalid = new DistributionCreateRequest(14, LocalDate.now(ZoneId.of("Asia/Seoul")));

		assertThatThrownBy(() -> distributionService.create(houseId, ownerId, invalid))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> {
					assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
					assertThat(e.getMessage()).startsWith("settlementCycle:");
				});
		assertThatThrownBy(() -> houseService.update(houseId, ownerId,
				new com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest(null, 2)))
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	private DistributionCreateRequest request() {
		return new DistributionCreateRequest(7, LocalDate.now(ZoneId.of("Asia/Seoul")));
	}

}
