package com.cmc.hackathon4.domain.house.service;

import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;

import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 집 단위 API 의 공통 권한 검사. 집이 없으면 HOUSE-404, 구성원이 아니면 HOUSE-403-1, 방장이 아니면 HOUSE-403. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseMemberValidator {

	private final HouseRepository houseRepository;
	private final HouseMemberRepository houseMemberRepository;

	/** 그 집의 구성원인지 확인하고 구성원 정보를 돌려준다. */
	public HouseMember getMember(Long houseId, Long userId) {
		if (!houseRepository.existsById(houseId)) {
			throw new BusinessException(ErrorCode.HOUSE_NOT_FOUND);
		}
		return houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_MEMBER));
	}

	/** 그 집의 방장인지 확인하고 구성원 정보를 돌려준다. */
	public HouseMember getOwner(Long houseId, Long userId) {
		HouseMember member = getMember(houseId, userId);
		if (!member.isOwner()) {
			throw new BusinessException(ErrorCode.HOUSE_OWNER_ONLY);
		}
		return member;
	}

}
