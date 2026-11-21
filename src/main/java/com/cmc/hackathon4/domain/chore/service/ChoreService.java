package com.cmc.hackathon4.domain.chore.service;

import java.util.HashSet;
import java.util.List;

import com.cmc.hackathon4.domain.chore.converter.ChoreConverter;
import com.cmc.hackathon4.domain.chore.dto.ChoreBulkSaveRequest;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.entity.Chore;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
	private final HouseMemberValidator houseMemberValidator;

	public ChorePresetResponse findPresets() {
		return ChoreConverter.toPresetResponse(PRESETS);
	}

	public ChoreListResponse findChores(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		return ChoreConverter.toListResponse(choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId));
	}

	/** 방장이 집안일 목록을 통째로 교체한다. WAITING 에는 짐이 없어 기존 행을 지우고 새로 저장해도 된다. */
	@Transactional
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
