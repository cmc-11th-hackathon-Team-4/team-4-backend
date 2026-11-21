package com.cmc.hackathon4.domain.house.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.house.converter.HomeConverter;
import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.notification.converter.NotificationConverter;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 홈 조회 API(내 메인 #17, 상대 상세 #18). 조회 전용. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

	private final HouseMemberValidator houseMemberValidator;
	private final HouseMemberRepository houseMemberRepository;
	private final PeriodRepository periodRepository;
	private final AssignmentRepository assignmentRepository;
	private final TransferRepository transferRepository;
	private final NotificationRepository notificationRepository;

	/** #17 내 메인 조회 */
	public HomeResponse getHome(Long houseId, Long userId) {
		House house = houseMemberValidator.getMember(houseId, userId).getHouse();

		// 집이 ACTIVE 가 아니면(시작 전) period 는 null. 그 외에는 READY 를 뺀 가장 최근 기간.
		Period period = house.getStatus() == HouseStatus.ACTIVE
				? periodRepository.findFirstByHouseIdAndStatusNotOrderByRoundDesc(houseId, PeriodStatus.READY)
						.orElse(null)
				: null;

		List<HouseMember> houseMembers = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
		Map<Long, User> users = houseMembers.stream().map(HouseMember::getUser)
				.collect(Collectors.toMap(User::getId, Function.identity()));

		// 기간의 짐 전체를 한 번에 가져와 메모리에서 나눈다.
		List<Assignment> todos = period == null ? List.of()
				: assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId()).stream()
						.filter(a -> a.getStatus() == AssignmentStatus.TODO).toList();
		Map<Long, Integer> todoCounts = todos.stream().collect(
				Collectors.groupingBy(a -> a.getAssignee().getId(), Collectors.summingInt(a -> 1)));

		List<HomeResponse.MemberInfo> members = houseMembers.stream()
				.map(m -> HomeConverter.toMemberInfo(m.getUser(), todoCounts.getOrDefault(m.getUser().getId(), 0),
						m.getUser().getId().equals(userId)))
				.toList();
		HomeResponse.MeInfo me = HomeConverter.toMeInfo(users.get(userId), todoCounts.getOrDefault(userId, 0),
				(int) transferRepository.countByToUserIdAndHeartedAtIsNotNull(userId));

		return new HomeResponse(HomeConverter.toHouseInfo(house),
				period == null ? null : HomeConverter.toPeriodInfo(period), me, members,
				toMyAssignments(period, todos, userId, users), toReceivedRequests(period, userId),
				notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(userId).stream()
						.map(NotificationConverter::toResponse).toList());
	}

	/** 내 TODO 짐. 기간이 ENDED(결산 대기)면 빈 배열. */
	private List<HomeResponse.MyAssignment> toMyAssignments(Period period, List<Assignment> todos, Long userId,
			Map<Long, User> users) {
		if (period == null || period.isEnded()) {
			return List.of();
		}
		List<Assignment> mine = todos.stream().filter(a -> a.getAssignee().getId().equals(userId)).toList();
		if (mine.isEmpty()) {
			return List.of();
		}
		Map<Long, Transfer> pendingByAssignment = transferRepository
				.findAllByAssignmentIdInAndStatus(mine.stream().map(Assignment::getId).toList(), TransferStatus.PENDING)
				.stream().collect(Collectors.toMap(t -> t.getAssignment().getId(), Function.identity(), (a, b) -> a));
		return mine.stream().map(a -> {
			Transfer pending = pendingByAssignment.get(a.getId());
			return HomeConverter.toMyAssignment(a, pending,
					pending == null ? null : users.get(pending.getToUser().getId()));
		}).toList();
	}

	/** 내가 받은 PENDING 부탁 중 현재 기간의 것. */
	private List<HomeResponse.ReceivedRequest> toReceivedRequests(Period period, Long userId) {
		if (period == null) {
			return List.of();
		}
		return transferRepository.findAllByToUserIdAndStatusOrderByIdAsc(userId, TransferStatus.PENDING).stream()
				.filter(t -> t.getAssignment().getPeriod().getId().equals(period.getId()))
				.sorted((a, b) -> Long.compare(b.getId(), a.getId())).map(HomeConverter::toReceivedRequest).toList();
	}

}
