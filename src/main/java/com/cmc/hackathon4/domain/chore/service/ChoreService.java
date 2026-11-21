package com.cmc.hackathon4.domain.chore.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
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
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import com.cmc.hackathon4.domain.transfer.repository.TransferRepository;
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

	private static final int MAX_CHORES_PER_HOUSE = 10;
	private static final int MAX_ASSIGNMENTS_PER_USER = 5;

	/** 집안일 고르기 화면의 선택지. DB 가 아니라 서버 상수다. */
	private static final List<String> PRESETS = List.of("빨래 널기", "설거지", "분리수거", "욕실 청소", "세탁기 돌리기", "빨래 개기",
			"청소기 돌리기", "바닥 닦기", "일반쓰레기 버리기", "음식물쓰레기 버리기");

	private final ChoreRepository choreRepository;
	private final TransferRepository transferRepository;
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
	 * 구성원이 집안일을 추가한다. 중복 이름 검사 뒤 한도를 확인하고, 모두 통과해야 저장한다.
	 * WAITING 이면 활성 집안일이 10개 미만일 때만 만들고 assignment 는 null 이다.
	 * ACTIVE 이고 진행 중(ACTIVE) 기간이 있으면 그 기간의 남은 짐이 집 전체 10개 미만, 추가한 사람 5개 미만일 때만 만든다.
	 * 이때 집안일은 그 기간에만 유효한(temporary) 것으로 만들어 추가한 사람의 짐을 바로 만들고, 기간이 끝나면 비활성화된다.
	 * ACTIVE 인데 진행 중인 기간이 없지만 지난 기간이 있으면(기간 사이) 활성 집안일이 10개 미만일 때만 임시(temporary)
	 * 집안일로 만들고 assignment 는 null 이다. 준비해 둔 다음 기간(READY)이 있으면 새 집안일이 빠지지 않도록 그 배분을 다시 만든다.
	 * DISTRIBUTING 이면 배분 미리보기가 어긋나므로 HOUSE-409-2, 기간이 아예 없으면 PERIOD-404 이다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ChoreCreateResponse createChore(Long houseId, Long userId, ChoreCreateRequest request) {
		houseMemberValidator.getMember(houseId, userId);
		House house = houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		if (house.getStatus() == HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
		}
		String name = request.name().trim();
		if (choreRepository.existsByHouseIdAndNameAndActiveTrue(houseId, name)) {
			throw new BusinessException(ErrorCode.CHORE_DUPLICATED);
		}
		if (house.getStatus() == HouseStatus.WAITING) {
			if (choreRepository.countByHouseIdAndActiveTrue(houseId) >= MAX_CHORES_PER_HOUSE) {
				throw new BusinessException(ErrorCode.CHORE_LIMIT_EXCEEDED);
			}
			Chore chore = choreRepository.save(ChoreConverter.toEntity(house, name));
			return ChoreConverter.toCreateResponse(chore, null);
		}

		Period period = periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).orElse(null);
		if (period == null) {
			if (periodRepository.findFirstByHouseIdOrderByRoundDesc(houseId).isEmpty()) {
				throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND);
			}
			// 기간 사이: 다음 기간에 배분되고 그 기간이 끝나면 비활성화되는 임시 집안일로 만든다
			if (choreRepository.countByHouseIdAndActiveTrue(houseId) >= MAX_CHORES_PER_HOUSE) {
				throw new BusinessException(ErrorCode.CHORE_LIMIT_EXCEEDED);
			}
			Chore chore = choreRepository.save(ChoreConverter.toTemporaryEntity(house, name));
			// refreshNextDistribution 이 영속성 컨텍스트를 비우므로 응답을 먼저 만든다
			ChoreCreateResponse response = ChoreConverter.toCreateResponse(chore, null);
			periodService.refreshNextDistribution(houseId);
			return response;
		}
		if (assignmentRepository.countByPeriodIdAndStatusAndDeletedAtIsNull(period.getId(),
				AssignmentStatus.TODO) >= MAX_CHORES_PER_HOUSE) {
			throw new BusinessException(ErrorCode.CHORE_LIMIT_EXCEEDED);
		}
		if (assignmentRepository.countByPeriodIdAndAssigneeIdAndStatusAndDeletedAtIsNull(period.getId(), userId,
				AssignmentStatus.TODO) >= MAX_ASSIGNMENTS_PER_USER) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Chore chore = choreRepository.save(ChoreConverter.toTemporaryEntity(house, name));
		Assignment assignment = assignmentRepository.save(new Assignment(period, chore, user, false));
		return ChoreConverter.toCreateResponse(chore, assignment);
	}

	/**
	 * 구성원 누구나 집안일을 삭제(비활성화)한다. 진행 중(ACTIVE) 기간의 남은(TODO) 짐은 담당자의 짐에서 빠지고
	 * 걸려 있던 부탁(PENDING)은 취소된다. 완료한 짐은 기록으로 남는다. 다음 기간(READY)에 미리 배분된 짐은 지운다.
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void deleteChore(Long houseId, Long userId, Long choreId) {
		houseMemberValidator.getMember(houseId, userId);
		House house = houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		if (house.getStatus() == HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
		}
		Chore chore = choreRepository.findById(choreId)
				.filter(found -> found.getHouse().getId().equals(houseId) && found.isActive())
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));
		chore.deactivate();

		Instant now = Instant.now();
		periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.ACTIVE).ifPresent(period -> {
			List<Assignment> todo = assignmentRepository.findAllByPeriodIdAndChoreIdAndStatus(period.getId(), choreId,
					AssignmentStatus.TODO);
			if (todo.isEmpty()) {
				return;
			}
			transferRepository
					.findAllByAssignmentIdInAndStatus(todo.stream().map(Assignment::getId).toList(),
							TransferStatus.PENDING)
					.forEach(transfer -> transfer.cancel(now));
			todo.forEach(assignment -> assignment.delete(now));
		});
		// 영속성 컨텍스트를 비우는 삭제라 마지막에 한다 (위 변경은 flushAutomatically 로 먼저 반영된다)
		periodRepository.findByHouseIdAndStatus(houseId, PeriodStatus.READY)
				.ifPresent(next -> assignmentRepository.deleteAllByPeriodIdAndChoreId(next.getId(), choreId));
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
		List<String> names = request.names().stream().map(String::trim).toList();
		if (new HashSet<>(names).size() != names.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		choreRepository.deleteAll(choreRepository.findAllByHouseId(houseId));
		choreRepository.flush();
		List<Chore> saved = choreRepository.saveAll(names.stream().map(name -> ChoreConverter.toEntity(house, name)).toList());
		return ChoreConverter.toListResponse(saved);
	}

}
