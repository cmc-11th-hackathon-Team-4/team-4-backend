package com.cmc.hackathon4.domain.house.service;

import java.time.LocalDate;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.dto.MemberDetailResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class HomeServiceTest {

	@Autowired
	HomeService homeService;
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
	NotificationService notificationService;

	User me;
	User other;
	House house;

	@BeforeEach
	void setUp() {
		me = userRepository.save(new User("device-1", "수아", CharacterType.CHARACTER_1));
		other = userRepository.save(new User("device-2", "민지", CharacterType.CHARACTER_3));
		house = houseRepository.save(new House("302호 우리집", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, me, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, other, MemberRole.MEMBER));
	}

	private Period period(int round, PeriodStatus status) {
		Period period = new Period(house, round, LocalDate.of(2026, 11, 23).plusWeeks(round),
				LocalDate.of(2026, 11, 29).plusWeeks(round));
		if (status != PeriodStatus.READY) {
			period.start();
		}
		if (status == PeriodStatus.ENDED) {
			period.end(java.time.Instant.now());
		}
		return periodRepository.save(period);
	}

	private Assignment assignment(Period period, User assignee, String name) {
		Chore chore = choreRepository.save(new Chore(house, name));
		return assignmentRepository.save(new Assignment(period, chore, assignee, false));
	}

	@Test
	void 집이_ACTIVE_가_아니면_period_는_null_이고_짐은_비어있다() {
		period(1, PeriodStatus.ACTIVE);

		HomeResponse response = homeService.getHome(house.getId(), me.getId());

		assertThat(response.period()).isNull();
		assertThat(response.myAssignments()).isEmpty();
		assertThat(response.members()).hasSize(2);
	}

	@Test
	void period_는_READY_를_제외한_가장_최근_기간이다() {
		house.activate();
		Period active = period(1, PeriodStatus.ACTIVE);
		period(2, PeriodStatus.READY);
		assignment(active, me, "설거지");

		HomeResponse response = homeService.getHome(house.getId(), me.getId());

		assertThat(response.period().id()).isEqualTo(active.getId());
		assertThat(response.period().status()).isEqualTo(PeriodStatus.ACTIVE);
		assertThat(response.myAssignments()).hasSize(1);
		assertThat(response.me().assignmentCount()).isEqualTo(1);
	}

	@Test
	void 기간이_ENDED_면_결산_대기라_myAssignments_는_빈_배열이다() {
		house.activate();
		Period ended = period(1, PeriodStatus.ENDED);
		period(2, PeriodStatus.READY);
		assignment(ended, me, "설거지");

		HomeResponse response = homeService.getHome(house.getId(), me.getId());

		assertThat(response.period().status()).isEqualTo(PeriodStatus.ENDED);
		assertThat(response.myAssignments()).isEmpty();
	}

	@Test
	void myAssignments_는_내_TODO_짐만_담고_부탁_중이면_pendingRequest_가_있다() {
		house.activate();
		Period active = period(1, PeriodStatus.ACTIVE);
		Assignment pending = assignment(active, me, "빨래");
		Assignment done = assignment(active, me, "설거지");
		done.complete(java.time.Instant.now());
		assignment(active, other, "분리수거");
		transferRepository.save(Transfer.request(pending, me, other));

		HomeResponse response = homeService.getHome(house.getId(), me.getId());

		assertThat(response.myAssignments()).hasSize(1);
		HomeResponse.MyAssignment mine = response.myAssignments().get(0);
		assertThat(mine.id()).isEqualTo(pending.getId());
		assertThat(mine.pendingRequest().toUserId()).isEqualTo(other.getId());
		assertThat(mine.pendingRequest().toName()).isEqualTo("민지");
		assertThat(response.members()).extracting(HomeResponse.MemberInfo::assignmentCount).containsExactly(1, 1);
	}

	@Test
	void receivedRequests_는_현재_기간의_PENDING_부탁만_담는다() {
		house.activate();
		Period old = period(1, PeriodStatus.ENDED);
		Period active = period(2, PeriodStatus.ACTIVE);
		Assignment oldOne = assignment(old, other, "지난 짐");
		Assignment current = assignment(active, other, "바닥 닦기");
		Assignment accepted = assignment(active, other, "설거지");
		transferRepository.save(Transfer.request(oldOne, other, me));
		Transfer pending = transferRepository.save(Transfer.request(current, other, me));
		Transfer done = transferRepository.save(Transfer.request(accepted, other, me));
		done.accept(java.time.Instant.now());

		HomeResponse response = homeService.getHome(house.getId(), me.getId());

		assertThat(response.receivedRequests()).hasSize(1);
		assertThat(response.receivedRequests().get(0).transferId()).isEqualTo(pending.getId());
		assertThat(response.receivedRequests().get(0).choreName()).isEqualTo("바닥 닦기");
		assertThat(response.receivedRequests().get(0).fromName()).isEqualTo("민지");
	}

	@Test
	void notifications_는_안_읽은_알림만_담는다() {
		house.activate();
		Period active = period(1, PeriodStatus.ACTIVE);
		Assignment a = assignment(active, me, "설거지");
		Transfer transfer = transferRepository.save(Transfer.relieve(a, me, other, java.time.Instant.now()));
		notificationService.send(me, other, transfer, NotificationType.CHORE_RELIEVED, "민지가 설거지를 덜어갔어요");

		HomeResponse response = homeService.getHome(house.getId(), me.getId());
		HomeResponse otherResponse = homeService.getHome(house.getId(), other.getId());

		assertThat(response.notifications()).hasSize(1);
		assertThat(response.notifications().get(0).message()).isEqualTo("민지가 설거지를 덜어갔어요");
		assertThat(otherResponse.notifications()).isEmpty();
	}

	@Test
	void 구성원이_아니면_조회할_수_없다() {
		User stranger = userRepository.save(new User("device-3", "철수", CharacterType.CHARACTER_2));

		assertThatThrownBy(() -> homeService.getHome(house.getId(), stranger.getId()))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void 상대_상세는_남은_TODO_짐만_담고_부탁_중인_짐은_requestPending_이다() {
		house.activate();
		Period active = period(1, PeriodStatus.ACTIVE);
		Assignment pending = assignment(active, other, "바닥 닦기");
		assignment(active, other, "분리수거");
		Assignment done = assignment(active, other, "설거지");
		done.complete(java.time.Instant.now());
		assignment(active, me, "내 짐");
		transferRepository.save(Transfer.request(pending, other, me));

		MemberDetailResponse response = homeService.getMemberDetail(house.getId(), me.getId(), other.getId());

		assertThat(response.name()).isEqualTo("민지");
		assertThat(response.assignmentCount()).isEqualTo(2);
		assertThat(response.assignments()).extracting(MemberDetailResponse.MemberAssignment::name)
				.containsExactly("바닥 닦기", "분리수거");
		assertThat(response.assignments()).extracting(MemberDetailResponse.MemberAssignment::requestPending)
				.containsExactly(true, false);
	}

	@Test
	void 상대_상세는_대상이_없거나_구성원이_아니면_실패한다() {
		house.activate();
		period(1, PeriodStatus.ACTIVE);
		User stranger = userRepository.save(new User("device-3", "철수", CharacterType.CHARACTER_2));

		assertThatThrownBy(() -> homeService.getMemberDetail(house.getId(), me.getId(), stranger.getId()))
				.isInstanceOf(BusinessException.class).hasMessageContaining("구성원");
		assertThatThrownBy(() -> homeService.getMemberDetail(house.getId(), me.getId(), 999_999L))
				.isInstanceOf(BusinessException.class).hasMessageContaining("사용자");
	}

}
