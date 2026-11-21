package com.cmc.hackathon4.domain.period.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.period.converter.PeriodConverter;
import com.cmc.hackathon4.domain.period.dto.CarryOverRequest;
import com.cmc.hackathon4.domain.period.dto.CarryOverResponse;
import com.cmc.hackathon4.domain.period.dto.NextPeriodResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodStartResponse;
import com.cmc.hackathon4.domain.period.dto.SettlementResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결산 API(#27 기간 늘리기, #28 기간 종료, #30 결산 조회, #31 이월 선택, #32·#38·#33 다음 주기). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeriodService {

	private final HouseMemberValidator houseMemberValidator;
	private final HouseMemberRepository houseMemberRepository;
	private final PeriodRepository periodRepository;
	private final NotificationService notificationService;
	private final TransferRepository transferRepository;
	private final AssignmentRepository assignmentRepository;
	private final ChoreRepository choreRepository;
	private final ChoreDistributor choreDistributor;

	/** 진행 중인 기간의 결산일을 days 만큼 늦추고 방장 제외 전원에게 PERIOD_EXTENDED 알림을 보낸다. */
	@Transactional
	public PeriodExtendResponse extend(Long houseId, Long userId, PeriodExtendRequest request) {
		// 요청 검증이 권한 검사보다 먼저였던 기존 순서(@Valid)를 유지한다
		if (!List.of(1, 3, 7).contains(request.days())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "days: 늘릴 일수는 1, 3, 7 중 하나여야 합니다.");
		}
		HouseMember owner = houseMemberValidator.getOwner(houseId, userId);
		Period period = getActivePeriod(houseId);
		period.extend(request.days());

		String message = "결산일이 %d월 %d일로 늘어났어요".formatted(period.getEndDate().getMonthValue(),
				period.getEndDate().getDayOfMonth());
		notificationService.sendAll(membersExcept(houseId, userId), owner.getUser(), null,
				NotificationType.PERIOD_EXTENDED, message);
		return PeriodConverter.toExtendResponse(period);
	}

	/**
	 * 진행 중인 기간을 종료한다. PENDING 부탁은 전부 CANCELED, 전원(방장 포함)에게 PERIOD_ENDED 알림. 다음 기간은 만들지 않는다.
	 */
	@Transactional
	public PeriodEndResponse end(Long houseId, Long userId) {
		houseMemberValidator.getOwner(houseId, userId);
		Period period = getActivePeriod(houseId);
		endPeriod(period, Instant.now());
		return PeriodConverter.toEndResponse(period);
	}

	/** 결산일(endDate)이 today 보다 앞선 진행 중 기간의 id. 자동 종료 스케줄러가 쓴다. */
	public List<Long> findExpiredPeriodIds(LocalDate today) {
		return periodRepository.findAllByStatusAndEndDateBefore(PeriodStatus.ACTIVE, today)
				.stream()
				.map(Period::getId)
				.toList();
	}

	/**
	 * 결산일이 지난 기간을 #28 과 같은 방식으로 종료한다. 그 사이 방장이 직접 종료했거나 기간을 늘렸으면 아무것도 하지 않는다.
	 * @return 실제로 종료했으면 true
	 */
	@Transactional
	public boolean endIfExpired(Long periodId, LocalDate today) {
		Period period = periodRepository.findById(periodId).orElse(null);
		if (period == null || period.getStatus() != PeriodStatus.ACTIVE || !period.getEndDate().isBefore(today)) {
			return false;
		}
		endPeriod(period, Instant.now());
		return true;
	}

	private void endPeriod(Period period, Instant now) {
		period.end(now);
		transferRepository.findAllByPeriodIdAndStatus(period.getId(), TransferStatus.PENDING)
				.forEach(transfer -> transfer.cancel(now));

		List<User> everyone = houseMemberRepository.findAllByHouseIdOrderByIdAsc(period.getHouse().getId())
				.stream()
				.map(HouseMember::getUser)
				.toList();
		notificationService.sendAll(everyone, null, null, NotificationType.PERIOD_ENDED, "이번 주 결산이 도착했어요");
	}

	/** 종료된 기간의 결산 상세. 그 집의 구성원만, ENDED 기간만 조회할 수 있다. */
	public SettlementResponse getSettlement(Long periodId, Long userId) {
		Period period = getPeriod(periodId);
		Long houseId = period.getHouse().getId();
		houseMemberValidator.getMember(houseId, userId);
		if (!period.isEnded()) {
			throw new BusinessException(ErrorCode.PERIOD_NOT_ENDED);
		}
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
		List<Assignment> assignments = assignmentRepository.findAllByPeriodIdOrderByIdAsc(periodId);
		List<Transfer> accepted = transferRepository.findAllByPeriodIdAndStatus(periodId, TransferStatus.ACCEPTED);
		return PeriodConverter.toSettlementResponse(period, members, assignments, accepted, userId,
				isNextPeriodStarted(period));
	}

	/**
	 * 내 못 한 일의 이월 선택(KEEP/RELEASE)을 저장한다. 기간이 ENDED 이고 다음 기간이 시작되기 전일 때만 가능하며, READY 인 다음 기간이 이미 있으면 그 배분을 새 선택으로 다시 만든다.
	 */
	@Transactional
	public CarryOverResponse saveCarryOvers(Long periodId, Long userId, CarryOverRequest request) {
		Period period = getPeriod(periodId);
		Long houseId = period.getHouse().getId();
		houseMemberValidator.getMember(houseId, userId);
		if (!period.isEnded()) {
			throw new BusinessException(ErrorCode.PERIOD_NOT_ENDED);
		}
		if (isNextPeriodStarted(period)) {
			throw new BusinessException(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
		}

		Map<Long, Assignment> myUndone = assignmentRepository
				.findAllByPeriodIdAndAssigneeIdAndStatusOrderByIdAsc(periodId, userId, AssignmentStatus.TODO)
				.stream()
				.collect(Collectors.toMap(Assignment::getId, Function.identity()));
		if (request.choices().stream().anyMatch(choice -> !myUndone.containsKey(choice.assignmentId()))) {
			// 남의 짐뿐 아니라 이미 완료한 내 짐도 여기로 온다
			throw new BusinessException(ErrorCode.ASSIGNMENT_NOT_MINE, "내가 못 한 일만 이월할 수 있습니다.");
		}
		request.choices().forEach(choice -> myUndone.get(choice.assignmentId()).chooseCarry(choice.choice()));

		periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)
				.filter(ready -> ready.getRound() > period.getRound())
				.ifPresent(ready -> createNextAssignments(houseId, ready.getId(), periodId));
		return new CarryOverResponse(request.choices());
	}

	/**
	 * 다음 기간(READY)의 배분을 지우고 새로 만든다. 직전(ENDED) 기간의 못 한 일 중 KEEP(미선택 포함)은 그 담당자에게 먼저 고정(carriedOver)하고, 나머지 활성
	 * 집안일은 총 개수가 균등해지도록 랜덤 배분한다. 영속성 컨텍스트가 비워지므로 엔티티를 다시 조회해서 쓴다.
	 */
	private List<Assignment> createNextAssignments(Long houseId, Long nextPeriodId, Long endedPeriodId) {
		assignmentRepository.deleteAllByPeriodId(nextPeriodId);

		Period next = getPeriod(nextPeriodId);
		List<User> users = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId)
				.stream()
				.map(HouseMember::getUser)
				.toList();
		Map<Long, User> userById = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
		List<Chore> chores = choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId);
		Set<Long> activeChoreIds = chores.stream().map(Chore::getId).collect(Collectors.toSet());

		List<Assignment> carried = assignmentRepository.findAllByPeriodIdOrderByIdAsc(endedPeriodId)
				.stream()
				.filter(Assignment::isKeptForNextPeriod)
				.filter(a -> activeChoreIds.contains(a.getChore().getId())
						&& userById.containsKey(a.getAssignee().getId()))
				.map(a -> new Assignment(next, a.getChore(), userById.get(a.getAssignee().getId()), true))
				.toList();
		Set<Long> carriedChoreIds = carried.stream().map(a -> a.getChore().getId()).collect(Collectors.toSet());
		Map<User, Integer> fixedCounts = new HashMap<>();
		carried.forEach(a -> fixedCounts.merge(a.getAssignee(), 1, Integer::sum));

		List<Chore> pool = chores.stream().filter(chore -> !carriedChoreIds.contains(chore.getId())).toList();
		List<Assignment> distributed = choreDistributor.distribute(users, fixedCounts, pool)
				.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue().stream().map(chore -> new Assignment(next, chore, entry.getKey(), false)))
				.toList();

		List<Assignment> assignments = new ArrayList<>(carried);
		assignments.addAll(distributed);
		return assignmentRepository.saveAll(assignments);
	}

	/**
	 * 직전 기간(ENDED)을 기준으로 다음 회차 기간(READY)을 만들고, 이미 있으면 배분만 새로 만든다. 이월(KEEP)된 짐은 그대로 두고 나머지만 다시 섞인다.
	 */
	@Transactional
	public NextPeriodResponse createNext(Long houseId, Long userId) {
		houseMemberValidator.getOwner(houseId, userId);
		Period previous = getPreviousEndedPeriod(houseId);
		Period next = findReadyNext(houseId, previous).orElseGet(() -> {
			LocalDate startDate = previous.getEndDate().plusDays(1);
			LocalDate endDate = previous.getHouse().getSettlementCycle().endDateFrom(startDate);
			return periodRepository.save(new Period(previous.getHouse(), previous.getRound() + 1, startDate, endDate));
		});

		List<Assignment> assignments = createNextAssignments(houseId, next.getId(), previous.getId());
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
		return PeriodConverter.toNextPeriodResponse(getPeriod(next.getId()), members, assignments, userId);
	}

	/** 서버에 저장된 다음 기간 배분을 구성원 누구나 조회한다. 아직 없으면 에러 대신 prepared=false. */
	public NextPeriodResponse getNext(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		Period previous = getPreviousEndedPeriod(houseId);
		return findReadyNext(houseId, previous).map(next -> {
			List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
			List<Assignment> assignments = assignmentRepository.findAllByPeriodIdOrderByIdAsc(next.getId());
			return PeriodConverter.toNextPeriodResponse(next, members, assignments, userId);
		}).orElseGet(NextPeriodResponse::notPrepared);
	}

	/** 저장된 다음 기간(READY)을 ACTIVE 로 시작하고 방장 제외 전원에게 PERIOD_STARTED 알림을 보낸다. */
	@Transactional
	public PeriodStartResponse startNext(Long houseId, Long userId) {
		HouseMember owner = houseMemberValidator.getOwner(houseId, userId);
		if (periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).isPresent()) {
			throw new BusinessException(ErrorCode.PERIOD_NEXT_ALREADY_STARTED);
		}
		Period previous = periodRepository.findFirstByHouseIdAndStatusOrderByRoundDesc(houseId, PeriodStatus.ENDED)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NEXT_NOT_READY));
		Period next = findReadyNext(houseId, previous)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NEXT_NOT_READY));
		next.start();

		notificationService.sendAll(membersExcept(houseId, userId), owner.getUser(), null,
				NotificationType.PERIOD_STARTED, "새 집안일이 배분됐어요");
		return PeriodConverter.toPeriodStartResponse(next);
	}

	/**
	 * 다음 주기 API 의 기준이 되는 직전 ENDED 기간. 진행 중(ACTIVE)인 기간이 1회차면 아직 종료되지 않은 것(PERIOD-409-1), 2회차 이후면 다음 기간이 이미 시작된 것(PERIOD-409-2)이다.
	 */
	private Period getPreviousEndedPeriod(Long houseId) {
		periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).ifPresent(active -> {
			throw new BusinessException(active.getRound() > 1 ? ErrorCode.PERIOD_NEXT_ALREADY_STARTED
					: ErrorCode.PERIOD_NOT_ENDED);
		});
		return periodRepository.findFirstByHouseIdAndStatusOrderByRoundDesc(houseId, PeriodStatus.ENDED)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NOT_ENDED));
	}

	private Optional<Period> findReadyNext(Long houseId, Period previous) {
		return periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)
				.filter(ready -> ready.getRound() > previous.getRound());
	}

	private Period getPeriod(Long periodId) {
		return periodRepository.findById(periodId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NOT_FOUND));
	}

	/** 이 기간보다 뒤 회차 중 READY 가 아닌(시작된) 기간이 있는지 */
	private boolean isNextPeriodStarted(Period period) {
		return periodRepository.findFirstByHouseIdAndStatusNotOrderByRoundDesc(period.getHouse().getId(),
				PeriodStatus.READY).filter(latest -> latest.getRound() > period.getRound()).isPresent();
	}

	private Period getActivePeriod(Long houseId) {
		return periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NOT_FOUND));
	}

	private List<User> membersExcept(Long houseId, Long userId) {
		return houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId)
				.stream()
				.map(HouseMember::getUser)
				.filter(user -> !user.getId().equals(userId))
				.toList();
	}

}
