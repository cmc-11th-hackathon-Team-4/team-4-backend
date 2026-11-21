package com.cmc.hackathon4.domain.chore.service;

import java.util.HashSet;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.chore.converter.ChoreConverter;
import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreCreateResponse;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.period.service.PeriodService;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChoreService {

	/** 집안일 고르기 화면의 선택지. DB 가 아니라 서버 상수다. */
	private static final List<String> PRESETS = List.of("빨래 널기", "설거지", "분리수거", "욕실 청소", "세탁기 돌리기", "빨래 개기",
			"청소기 돌리기", "바닥 닦기", "일반쓰레기 버리기", "음식물쓰레기 버리기");

	private final ChoreRepository choreRepository;
	private final HouseRepository houseRepository;
	private final PeriodRepository periodRepository;
	private final AssignmentRepository assignmentRepository;
	private final UserRepository userRepository;
	private final HouseMemberValidator houseMemberValidator;
	private final PeriodService periodService;

	public ChorePresetResponse findPresets() {
		return ChoreConverter.toPresetResponse(PRESETS);
	}

	public ChoreListResponse findChores(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		return ChoreConverter.toListResponse(choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId));
	}

	/**
	 * 구성원이 집안일을 추가한다. 집이 ACTIVE 이고 진행 중인 기간이 있으면 추가한 사람의 짐을 바로 만든다.
	 * 노션에 없는 경우의 선택: DISTRIBUTING 이면 배분 미리보기가 어긋나므로 HOUSE-409-2, ACTIVE 인데 진행 중(ACTIVE)
	 * 기간이 없지만 지난 기간(ENDED 등)이 있으면 집안일만 만들고 assignment 는 null, 기간이 아예 없으면 PERIOD-404.
	 * 그때 준비해 둔 다음 기간(READY)이 있으면 새 집안일이 빠지지 않도록 그 배분을 다시 만든다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ChoreCreateResponse createChore(Long houseId, Long userId, ChoreCreateRequest request) {
		houseMemberValidator.getMember(houseId, userId);
		House house = houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		if (house.getStatus() == HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
		}
		if (choreRepository.existsByHouseIdAndNameAndActiveTrue(houseId, request.name())) {
			throw new BusinessException(ErrorCode.CHORE_DUPLICATED);
		}
		Chore chore = choreRepository.save(ChoreConverter.toEntity(house, request.name()));
		if (house.getStatus() == HouseStatus.WAITING) {
			return ChoreConverter.toCreateResponse(chore, null);
		}

		Period period = periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).orElse(null);
		if (period == null) {
			if (periodRepository.findFirstByHouseIdOrderByRoundDesc(houseId).isEmpty()) {
				throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND);
			}
			ChoreCreateResponse response = ChoreConverter.toCreateResponse(chore, null);
			periodService.refreshNextDistribution(houseId);
			return response;
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Assignment assignment = assignmentRepository.save(new Assignment(period, chore, user, false));
		return ChoreConverter.toCreateResponse(chore, assignment);
	}

	/** 방장이 집안일 목록을 통째로 교체한다. WAITING 에는 짐이 없어 기존 행을 지우고 새로 저장해도 된다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ChoreListResponse replaceChores(Long houseId, Long userId, ChoreBulkSaveRequest request) {
		houseMemberValidator.getOwner(houseId, userId);
		House house = houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		if (house.getStatus() == HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
		}
		if (house.getStatus() == HouseStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.HOUSE_ALREADY_STARTED);
		}
		List<String> names = request.names();
		if (new HashSet<>(names).size() != names.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		choreRepository.deleteAll(choreRepository.findAllByHouseId(houseId));
		choreRepository.flush();
		List<Chore> saved = choreRepository.saveAll(names.stream().map(name -> ChoreConverter.toEntity(house, name)).toList());
		return ChoreConverter.toListResponse(saved);
	}

}
