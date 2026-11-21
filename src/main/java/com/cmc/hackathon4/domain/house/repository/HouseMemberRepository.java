package com.cmc.hackathon4.domain.house.repository;

import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.house.entity.HouseMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {

	/** 사용자가 속한 집(한 사용자는 한 집만). house 를 함께 가져온다. */
	@EntityGraph(attributePaths = "house")
	Optional<HouseMember> findByUserId(Long userId);

	boolean existsByUserId(Long userId);

	Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);

	/** 집의 구성원 전체(참여 순). user 를 함께 가져온다. */
	@EntityGraph(attributePaths = "user")
	List<HouseMember> findAllByHouseIdOrderByIdAsc(Long houseId);

	long countByHouseId(Long houseId);

}
