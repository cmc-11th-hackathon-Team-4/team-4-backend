package com.cmc.hackathon4.domain.period.service;

import java.util.List;

import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.period.converter.PeriodConverter;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendRequest;
import com.cmc.hackathon4.domain.period.dto.PeriodExtendResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
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
