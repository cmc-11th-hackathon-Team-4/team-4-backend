package com.cmc.hackathon4.domain.period.service;

import java.time.Instant;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.period.converter.PeriodConverter;
import com.cmc.hackathon4.domain.period.dto.PeriodEndResponse;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
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

	/** 진행 중인 기간의 결산일을 days 만큼 늦추고 방장 제외 전원에게 PERIOD_EXTENDED 알림을 보낸다. */
	@Transactional
	public PeriodExtendResponse extend(Long houseId, Long userId, PeriodExtendRequest request) {
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
		Instant now = Instant.now();
		period.end(now);
		transferRepository.findAllByPeriodIdAndStatus(period.getId(), TransferStatus.PENDING)
				.forEach(transfer -> transfer.cancel(now));

		List<User> everyone = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId)
				.stream()
				.map(HouseMember::getUser)
				.toList();
		notificationService.sendAll(everyone, null, null, NotificationType.PERIOD_ENDED, "이번 주 결산이 도착했어요");
		return PeriodConverter.toEndResponse(period);
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
