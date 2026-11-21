package com.cmc.hackathon4.domain.period.repository;

import java.util.Optional;

import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodRepository extends JpaRepository<Period, Long> {

	/** 집의 READY 또는 ACTIVE 기간은 각각 최대 1개라 Optional 로 받는다. ENDED 에는 쓰지 않는다. */
	Optional<Period> findByHouseIdAndStatus(Long houseId, PeriodStatus status);

	/** 집의 가장 최근 회차 */
	Optional<Period> findFirstByHouseIdOrderByRoundDesc(Long houseId);

	/** 특정 상태를 뺀 가장 최근 회차. 홈은 READY 를 뺀 최신 기간을 쓴다. */
	Optional<Period> findFirstByHouseIdAndStatusNotOrderByRoundDesc(Long houseId, PeriodStatus status);

	/** 특정 상태의 가장 최근 회차 (예: 직전 ENDED 기간) */
	Optional<Period> findFirstByHouseIdAndStatusOrderByRoundDesc(Long houseId, PeriodStatus status);

}
