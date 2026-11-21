package com.cmc.hackathon4.domain.house.service;

import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 같은 집에 대한 쓰기가 겹치지 않도록 집 행을 잠근다(PESSIMISTIC_WRITE). 잠금은 호출한 쪽 트랜잭션이 끝날 때 풀리므로 쓰기 트랜잭션 안에서만
 * 부른다. 잠금을 기다린 뒤의 최신 상태를 읽도록, 그 집의 다른 엔티티를 읽기 전에 부른다. 집이 없으면 HOUSE-404.
 */
@Component
@RequiredArgsConstructor
public class HouseLocker {

	private final HouseRepository houseRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public House lock(Long houseId) {
		return houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
	}

}
