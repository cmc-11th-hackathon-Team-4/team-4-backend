package com.cmc.hackathon4.domain.transfer.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.transfer.entity.Transfer;
import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

	/** 짐에 특정 상태의 이동이 있는지. "짐 하나에 PENDING 부탁은 하나만" 검사에 쓴다. */
	boolean existsByAssignmentIdAndStatus(Long assignmentId, TransferStatus status);

	List<Transfer> findAllByAssignmentIdAndStatus(Long assignmentId, TransferStatus status);

	List<Transfer> findAllByAssignmentIdInAndStatus(Collection<Long> assignmentIds, TransferStatus status);

	/** 내가 받은 특정 상태의 이동(예: 부탁 받음 = PENDING). 짐·집안일·보낸 사람을 함께 가져온다. */
	@EntityGraph(attributePaths = {"assignment", "assignment.chore", "fromUser"})
	List<Transfer> findAllByToUserIdAndStatusOrderByIdAsc(Long toUserId, TransferStatus status);

	/** 기간 안의 특정 상태 이동 전체 (결산 집계, 기간 종료 시 PENDING 취소) */
	@Query("select t from Transfer t join fetch t.assignment a where a.period.id = :periodId and t.status = :status")
	List<Transfer> findAllByPeriodIdAndStatus(@Param("periodId") Long periodId,
			@Param("status") TransferStatus status);

	/** 내가 받은 하트 수: 내가 덜어간(toUser) 이동 중 하트가 찍힌 것 */
	long countByToUserIdAndHeartedAtIsNotNull(Long toUserId);

	/** 이동 하나. 짐(기간·집·집안일·담당자)과 보낸·받는 사람을 함께 가져온다. */
	@EntityGraph(attributePaths = {"assignment", "assignment.period", "assignment.chore", "assignment.assignee",
			"fromUser", "toUser"})
	Optional<Transfer> findWithDetailsById(Long id);

}
