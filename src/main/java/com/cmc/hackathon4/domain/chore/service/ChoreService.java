package com.cmc.hackathon4.domain.chore.service;

import java.util.List;

import com.cmc.hackathon4.domain.chore.converter.ChoreConverter;
import com.cmc.hackathon4.domain.chore.dto.ChoreListResponse;
import com.cmc.hackathon4.domain.chore.dto.ChorePresetResponse;
import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.service.HouseMemberValidator;
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
	private final HouseMemberValidator houseMemberValidator;

	public ChorePresetResponse findPresets() {
		return ChoreConverter.toPresetResponse(PRESETS);
	}

	public ChoreListResponse findChores(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		return ChoreConverter.toListResponse(choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId));
	}

}
