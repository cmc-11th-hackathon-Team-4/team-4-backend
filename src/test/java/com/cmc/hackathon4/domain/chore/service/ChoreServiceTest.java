package com.cmc.hackathon4.domain.chore.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreResponse;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
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
class ChoreServiceTest {

	@Autowired
	ChoreService choreService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;
	@Autowired
	HouseMemberRepository houseMemberRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	AssignmentRepository assignmentRepository;
	@Autowired
	ChoreRepository choreRepository;
	@Autowired
	TransferRepository transferRepository;

	User owner;
	User member;
	House house;

	@BeforeEach
	void setUp() {
		owner = userRepository.save(new User("device-1", "수아", CharacterType.CHARACTER_1));
		member = userRepository.save(new User("device-2", "민지", CharacterType.CHARACTER_3));
		house = houseRepository.save(new House("302호 우리집", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, owner, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, member, MemberRole.MEMBER));
	}

	@Test
	void 일괄_저장은_기존_목록을_통째로_교체한다() {
		choreService.replaceChores(house.getId(), owner.getId(), new ChoreBulkSaveRequest(List.of("설거지", "분리수거")));

		ChoreListResponse result = choreService.replaceChores(house.getId(), owner.getId(),
				new ChoreBulkSaveRequest(List.of("빨래 널기", "설거지")));

		assertThat(result.chores()).extracting(ChoreResponse::name).containsExactly("빨래 널기", "설거지");
		assertThat(choreService.findChores(house.getId(), member.getId()).chores()).hasSize(2);
	}

	@Test
	void 일괄_저장은_방장만_할_수_있다() {
		assertThatThrownBy(() -> choreService.replaceChores(house.getId(), member.getId(),
				new ChoreBulkSaveRequest(List.of("설거지")))).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.HOUSE_OWNER_ONLY);
	}

	@Test
	void 일괄_저장은_중복_이름이면_거부한다() {
		assertThatThrownBy(() -> choreService.replaceChores(house.getId(), owner.getId(),
				new ChoreBulkSaveRequest(List.of("설거지", "설거지")))).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void 일괄_저장은_DISTRIBUTING_이면_HOUSE_409_2() {
		house.startDistributing(SettlementCycle.WEEKLY);

		assertThatThrownBy(() -> choreService.replaceChores(house.getId(), owner.getId(),
				new ChoreBulkSaveRequest(List.of("설거지")))).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.HOUSE_DISTRIBUTING);
	}

	@Test
	void 일괄_저장은_ACTIVE_이면_HOUSE_409_4() {
		house.startDistributing(SettlementCycle.WEEKLY);
		house.activate();

		assertThatThrownBy(() -> choreService.replaceChores(house.getId(), owner.getId(),
				new ChoreBulkSaveRequest(List.of("설거지")))).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.HOUSE_ALREADY_STARTED);
	}

	@Test
	void ACTIVE_집에서_추가하면_추가한_사람의_짐이_만들어진다() {
		house.startDistributing(SettlementCycle.WEEKLY);
		house.activate();
		LocalDate start = LocalDate.of(2026, 11, 23);
		Period period = new Period(house, 1, start, SettlementCycle.WEEKLY.endDateFrom(start));
		period.start();
		periodRepository.save(period);

		ChoreCreateResponse response = choreService.createChore(house.getId(), member.getId(),
				new ChoreCreateRequest("음식물 쓰레기"));

		assertThat(response.chore().name()).isEqualTo("음식물 쓰레기");
		assertThat(response.assignment()).isNotNull();
		assertThat(response.assignment().status()).isEqualTo(AssignmentStatus.TODO);
		assertThat(response.assignment().carriedOver()).isFalse();
		assertThat(response.assignment().choreId()).isEqualTo(response.chore().id());
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId())).singleElement()
				.satisfies(a -> assertThat(a.getAssignee().getId()).isEqualTo(member.getId()));
	}

	@Test
	void WAITING_집에서_추가하면_짐은_null_이다() {
		ChoreCreateResponse response = choreService.createChore(house.getId(), member.getId(),
				new ChoreCreateRequest("설거지"));

		assertThat(response.chore().id()).isNotNull();
		assertThat(response.assignment()).isNull();
	}

	@Test
	void 배분_확인_중에는_집안일을_추가할_수_없다() {
		house.startDistributing(SettlementCycle.WEEKLY);

		assertThatThrownBy(() -> choreService.createChore(house.getId(), member.getId(),
				new ChoreCreateRequest("설거지"))).isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HOUSE_DISTRIBUTING));
		assertThat(choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(house.getId())).isEmpty();
	}

	@Test
	void 같은_이름의_집안일이_있으면_CHORE_409() {
		choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("설거지"));

		assertThatThrownBy(() -> choreService.createChore(house.getId(), member.getId(), new ChoreCreateRequest("설거지")))
				.isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.CHORE_DUPLICATED);
	}

	@Test
	void 앞뒤_공백만_다른_이름도_중복으로_본다() {
		ChoreCreateResponse created = choreService.createChore(house.getId(), owner.getId(),
				new ChoreCreateRequest(" 설거지 "));

		assertThat(created.chore().name()).isEqualTo("설거지");
		expectError(() -> choreService.createChore(house.getId(), member.getId(), new ChoreCreateRequest("설거지 ")),
				ErrorCode.CHORE_DUPLICATED);
		expectError(() -> choreService.replaceChores(house.getId(), owner.getId(),
				new ChoreBulkSaveRequest(List.of("분리수거", "분리수거 "))), ErrorCode.INVALID_INPUT);
	}

	// ---- 집안일 삭제 / 한도 ----

	private Period startHouse() {
		house.startDistributing(SettlementCycle.WEEKLY);
		house.activate();
		LocalDate start = LocalDate.of(2026, 11, 23);
		Period period = new Period(house, 1, start, SettlementCycle.WEEKLY.endDateFrom(start));
		period.start();
		return periodRepository.save(period);
	}

	private Assignment addAssignment(Period period, User assignee, String name) {
		Chore chore = choreRepository.save(new Chore(house, name));
		return assignmentRepository.save(new Assignment(period, chore, assignee, false));
	}

	private void expectError(Runnable action, ErrorCode code) {
		assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
	}

	@Test
	void 시작한_뒤_추가한_집안일은_임시_집안일이다() {
		startHouse();

		ChoreCreateResponse response = choreService.createChore(house.getId(), member.getId(),
				new ChoreCreateRequest("음식물 쓰레기"));

		assertThat(choreRepository.findById(response.chore().id()).orElseThrow().isTemporary()).isTrue();
	}

	@Test
	void 집안일을_삭제하면_남은_짐이_담당자에게서_빠지고_걸린_부탁은_취소된다() {
		Period period = startHouse();
		Assignment assignment = addAssignment(period, member, "설거지");
		Transfer request = transferRepository.save(Transfer.request(assignment, member, owner));

		// 방장이 아닌 구성원도 삭제할 수 있다
		choreService.deleteChore(house.getId(), member.getId(), assignment.getChore().getId());

		assertThat(choreRepository.findById(assignment.getChore().getId()).orElseThrow().isActive()).isFalse();
		assertThat(assignmentRepository.findAllByPeriodIdAndAssigneeIdAndStatusOrderByIdAsc(period.getId(),
				member.getId(), AssignmentStatus.TODO)).isEmpty();
		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId())).isEmpty();
		assertThat(assignmentRepository.findWithDetailsById(assignment.getId())).isEmpty();
		assertThat(transferRepository.findById(request.getId()).orElseThrow().getStatus())
				.isEqualTo(TransferStatus.CANCELED);
	}

	@Test
	void 집안일을_삭제해도_완료한_짐은_기록으로_남는다() {
		Period period = startHouse();
		Assignment done = addAssignment(period, member, "설거지");
		done.complete(Instant.now());
		Assignment todo = addAssignment(period, member, "분리수거");

		choreService.deleteChore(house.getId(), owner.getId(), done.getChore().getId());

		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId())).extracting(Assignment::getId)
				.containsExactly(done.getId(), todo.getId());
		assertThat(choreService.findChores(house.getId(), owner.getId()).chores()).extracting(ChoreResponse::name)
				.containsExactly("분리수거");
	}

	@Test
	void 이미_삭제했거나_다른_집의_집안일은_CHORE_404() {
		Period period = startHouse();
		Assignment assignment = addAssignment(period, member, "설거지");
		House other = houseRepository.save(new House("다른 집", "ZZZZZZ"));
		Chore otherChore = choreRepository.save(new Chore(other, "남의 집일"));
		choreService.deleteChore(house.getId(), owner.getId(), assignment.getChore().getId());

		expectError(() -> choreService.deleteChore(house.getId(), owner.getId(), assignment.getChore().getId()),
				ErrorCode.CHORE_NOT_FOUND);
		expectError(() -> choreService.deleteChore(house.getId(), owner.getId(), otherChore.getId()),
				ErrorCode.CHORE_NOT_FOUND);
	}

	@Test
	void 배분_확인_중에는_집안일을_삭제할_수_없다() {
		Chore chore = choreRepository.save(new Chore(house, "설거지"));
		house.startDistributing(SettlementCycle.WEEKLY);

		expectError(() -> choreService.deleteChore(house.getId(), owner.getId(), chore.getId()),
				ErrorCode.HOUSE_DISTRIBUTING);
	}

	@Test
	void 다음_기간이_준비된_상태에서_삭제하면_미리_배분된_짐도_지워진다() {
		Period first = startHouse();
		first.end(Instant.now());
		LocalDate nextStart = first.getEndDate().plusDays(1);
		Period next = periodRepository
				.save(new Period(house, 2, nextStart, SettlementCycle.WEEKLY.endDateFrom(nextStart)));
		Assignment ready = addAssignment(next, member, "설거지");
		Assignment kept = addAssignment(next, owner, "분리수거");

		choreService.deleteChore(house.getId(), owner.getId(), ready.getChore().getId());

		assertThat(assignmentRepository.findAllByPeriodIdOrderByIdAsc(next.getId())).extracting(Assignment::getId)
				.containsExactly(kept.getId());
	}

	@Test
	void WAITING_집은_집안일이_10개면_추가할_수_없다() {
		for (int i = 1; i <= 10; i++) {
			choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("집안일" + i));
		}

		expectError(() -> choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("집안일11")),
				ErrorCode.CHORE_LIMIT_EXCEEDED);
		assertThat(choreRepository.countByHouseIdAndActiveTrue(house.getId())).isEqualTo(10);
	}

	@Test
	void ACTIVE_집은_남은_짐이_10개면_추가할_수_없고_완료한_짐은_세지_않는다() {
		Period period = startHouse();
		for (int i = 1; i <= 5; i++) {
			addAssignment(period, owner, "집안일" + i);
			addAssignment(period, member, "일" + i);
		}

		expectError(() -> choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("추가")),
				ErrorCode.CHORE_LIMIT_EXCEEDED);

		assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId()).get(0).complete(Instant.now());
		ChoreCreateResponse response = choreService.createChore(house.getId(), owner.getId(),
				new ChoreCreateRequest("추가"));
		assertThat(response.assignment()).isNotNull();
	}

	@Test
	void ACTIVE_집은_내_짐이_5개면_추가할_수_없다() {
		Period period = startHouse();
		for (int i = 1; i <= 5; i++) {
			addAssignment(period, member, "일" + i);
		}

		expectError(() -> choreService.createChore(house.getId(), member.getId(), new ChoreCreateRequest("추가")),
				ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		assertThat(choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("추가")).assignment())
				.isNotNull();
	}

	@Test
	void ACTIVE_인데_기간이_하나도_없으면_PERIOD_404() {
		house.startDistributing(SettlementCycle.WEEKLY);
		house.activate();

		expectError(() -> choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("추가")),
				ErrorCode.PERIOD_NOT_FOUND);
	}

}
