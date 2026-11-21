package com.cmc.hackathon4.domain.chore.repository;

import java.util.List;

import com.cmc.hackathon4.domain.chore.entity.Chore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoreRepository extends JpaRepository<Chore, Long> {

	List<Chore> findAllByHouseIdAndActiveTrueOrderByIdAsc(Long houseId);

	boolean existsByHouseIdAndNameAndActiveTrue(Long houseId, String name);

	long countByHouseIdAndActiveTrue(Long houseId);

	List<Chore> findAllByHouseId(Long houseId);

}
