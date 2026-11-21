package com.cmc.hackathon4.domain.period.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.period.converter.PeriodConverter;
import com.cmc.hackathon4.domain.period.dto.DistributionCreateRequest;
import com.cmc.hackathon4.domain.period.dto.DistributionResponse;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 온보딩 랜덤 배분(#15 만들기, #37 조회, #36 다시 섞기, #16 확정). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistributionService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final HouseMemberValidator houseMemberValidator;
	private final HouseRepository houseRepository;
	private final HouseMemberRepository houseMemberRepository;
	private final ChoreRepository choreRepository;
	private final PeriodRepository periodRepository;
	private final AssignmentRepository assignmentRepository;
	private final ChoreDistributor choreDistributor;

	/**
	 * 결산 주기·시작일을 저장하고 1회차 기간(READY)과 랜덤 배분을 만든다. 이미 DISTRIBUTING 이면 주기·날짜를 갱신하고 배분을 새로 만든다.
	 */
	@Transactional
	public DistributionResponse create(Long houseId, Long userId, DistributionCreateRequest request) {
		houseMemberValidator.getOwner(houseId, userId);
		House house = getHouse(houseId);
		if (house.getStatus() == HouseStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.HOUSE_ALREADY_STARTED);
		}
		if (request.startDate().isBefore(LocalDate.now(KST)) || choreRepository
				.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId).isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		Long readyPeriodId = periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)
				.map(Period::getId)
				.orElse(null);
		if (readyPeriodId != null) {
			// 영속성 컨텍스트가 비워지므로 이 뒤에 엔티티를 다시 조회한다.
			assignmentRepository.deleteAllByPeriodId(readyPeriodId);
		}

		house = getHouse(houseId);
		LocalDate endDate = request.settlementCycle().endDateFrom(request.startDate());
		house.startDistributing(request.settlementCycle());
		Period period;
		if (readyPeriodId == null) {
			period = periodRepository.save(new Period(house, 1, request.startDate(), endDate));
		} else {
			period = getReadyPeriod(houseId);
			period.reschedule(request.startDate(), endDate);
		}
		return createDistribution(house, period, userId);
	}

	/** 서버에 저장된 배분 결과를 구성원 누구나 조회한다. DISTRIBUTING 일 때만 가능. */
	public DistributionResponse get(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		House house = getDistributingHouse(houseId);
		Period period = getReadyPeriod(houseId);
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
		List<Assignment> assignments = assignmentRepository.findAllByPeriodIdOrderByIdAsc(period.getId());
		return PeriodConverter.toDistributionResponse(house, period, members, assignments, userId);
	}

	private House getDistributingHouse(Long houseId) {
		House house = getHouse(houseId);
		if (house.getStatus() != HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_NOT_DISTRIBUTING);
		}
		return house;
	}

	private House getHouse(Long houseId) {
		return houseRepository.findById(houseId).orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
	}

	private Period getReadyPeriod(Long houseId) {
		return periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NEXT_NOT_READY));
	}

	/** 비어 있는 READY 기간에 랜덤 배분을 만들어 저장하고 응답으로 만든다. */
	private DistributionResponse createDistribution(House house, Period period, Long requesterId) {
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(house.getId());
		List<Chore> chores = choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(house.getId());
		List<User> users = members.stream().map(HouseMember::getUser).toList();

		Map<User, List<Chore>> distributed = choreDistributor.distribute(users, new HashMap<>(), chores);
		List<Assignment> assignments = distributed.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue()
						.stream()
						.map(chore -> new Assignment(period, chore, entry.getKey(), false)))
				.toList();
		assignmentRepository.saveAll(assignments);
		return PeriodConverter.toDistributionResponse(house, period, members, assignments, requesterId);
	}

}
