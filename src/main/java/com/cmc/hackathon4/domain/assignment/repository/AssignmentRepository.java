package com.cmc.hackathon4.domain.assignment.repository;

import java.util.List;
import java.util.Optional;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	/** 기간의 짐 전체. chore, assignee 를 함께 가져온다. */
	@EntityGraph(attributePaths = {"chore", "assignee"})
	@Query("select a from Assignment a where a.period.id = :periodId and a.deletedAt is null order by a.id asc")
	List<Assignment> findAllByPeriodIdOrderByIdAsc(@Param("periodId") Long periodId);

	/** 기간에서 한 사람이 맡은 특정 상태의 짐. chore 를 함께 가져온다. */
	@EntityGraph(attributePaths = "chore")
	@Query("select a from Assignment a where a.period.id = :periodId and a.assignee.id = :assigneeId and a.status = :status and a.deletedAt is null order by a.id asc")
	List<Assignment> findAllByPeriodIdAndAssigneeIdAndStatusOrderByIdAsc(@Param("periodId") Long periodId,
			@Param("assigneeId") Long assigneeId, @Param("status") AssignmentStatus status);

	/** 기간의 짐을 모두 지운다(READY 기간의 배분을 다시 만들 때). 영속성 컨텍스트도 비운다. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Assignment a where a.period.id = :periodId")
	void deleteAllByPeriodId(@Param("periodId") Long periodId);

	/** 짐 하나. 기간·집·집안일·담당자를 함께 가져온다. */
	@EntityGraph(attributePaths = {"period", "period.house", "chore", "assignee"})
	@Query("select a from Assignment a where a.id = :id and a.deletedAt is null")
	Optional<Assignment> findWithDetailsById(@Param("id") Long id);

	/** 기간의 삭제되지 않은 TODO 짐 수 (집 전체 한도 검사) */
	long countByPeriodIdAndStatusAndDeletedAtIsNull(Long periodId, AssignmentStatus status);

	/** 기간에서 한 사람이 맡은 삭제되지 않은 TODO 짐 수 (개인 한도 검사) */
	long countByPeriodIdAndAssigneeIdAndStatusAndDeletedAtIsNull(Long periodId, Long assigneeId,
			AssignmentStatus status);

	/** 기간의 특정 집안일에 대한 삭제되지 않은 TODO 짐 (집안일 삭제 시 짐 빼기) */
	@Query("select a from Assignment a where a.period.id = :periodId and a.chore.id = :choreId and a.status = :status and a.deletedAt is null")
	List<Assignment> findAllByPeriodIdAndChoreIdAndStatus(@Param("periodId") Long periodId,
			@Param("choreId") Long choreId, @Param("status") AssignmentStatus status);

	/** 기간의 특정 집안일 짐을 지운다(READY 기간에서 집안일을 삭제할 때). */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Assignment a where a.period.id = :periodId and a.chore.id = :choreId")
	void deleteAllByPeriodIdAndChoreId(@Param("periodId") Long periodId, @Param("choreId") Long choreId);

}
