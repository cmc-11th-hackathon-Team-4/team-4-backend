package com.cmc.hackathon4.domain.chore.service;

import java.time.LocalDate;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.entity.SettlementCycle;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
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
	void 같은_이름의_집안일이_있으면_CHORE_409() {
		choreService.createChore(house.getId(), owner.getId(), new ChoreCreateRequest("설거지"));

		assertThatThrownBy(() -> choreService.createChore(house.getId(), member.getId(), new ChoreCreateRequest("설거지")))
				.isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.CHORE_DUPLICATED);
	}

}
