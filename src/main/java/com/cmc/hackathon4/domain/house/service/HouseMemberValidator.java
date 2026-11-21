package com.cmc.hackathon4.domain.house.service;

import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;

import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 집 단위 API 의 공통 권한 검사와 집 행 잠금. 집이 없으면 HOUSE-404, 구성원이 아니면 HOUSE-403-1, 방장이 아니면 HOUSE-403. */
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

	/**
	 * 같은 집에 대한 쓰기가 겹치지 않도록 집 행을 잠그고(PESSIMISTIC_WRITE) 돌려준다. 잠금은 호출한 쪽 트랜잭션이 끝날 때 풀리므로 쓰기 트랜잭션
	 * 안에서만 부른다(밖에서 부르면 바로 실패). 잠금을 기다린 뒤의 최신 상태를 읽도록, 그 집의 다른 엔티티를 읽기 전에 부른다. 집이 없으면 HOUSE-404.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public House lockHouse(Long houseId) {
		return houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
	}

}
