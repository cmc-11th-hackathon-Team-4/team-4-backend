package com.cmc.hackathon4.domain.house.repository;

import java.util.Optional;

import com.cmc.hackathon4.domain.house.entity.House;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseRepository extends JpaRepository<House, Long> {

	Optional<House> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

	/** 집 행을 잠그고 조회한다. 동시 참여로 정원(5명)을 넘는 것을 막을 때 쓴다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select h from House h where h.id = :id")
	Optional<House> findByIdForUpdate(@Param("id") Long id);

	/** 초대 코드로 집 행을 잠그고 조회한다. 잠그기 전에 읽은 값으로 정원을 판단하지 않도록 참여에서 쓴다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select h from House h where h.inviteCode = :inviteCode")
	Optional<House> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

}
