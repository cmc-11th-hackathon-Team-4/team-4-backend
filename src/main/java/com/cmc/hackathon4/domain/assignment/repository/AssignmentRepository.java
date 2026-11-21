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
	List<Assignment> findAllByPeriodIdOrderByIdAsc(Long periodId);

	/** 기간에서 한 사람이 맡은 특정 상태의 짐. chore 를 함께 가져온다. */
	@EntityGraph(attributePaths = "chore")
	List<Assignment> findAllByPeriodIdAndAssigneeIdAndStatusOrderByIdAsc(Long periodId, Long assigneeId,
			AssignmentStatus status);

	/** 기간의 짐을 모두 지운다(READY 기간의 배분을 다시 만들 때). 영속성 컨텍스트도 비운다. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Assignment a where a.period.id = :periodId")
	void deleteAllByPeriodId(@Param("periodId") Long periodId);

	/** 짐 하나. 기간·집·집안일·담당자를 함께 가져온다. */
	@EntityGraph(attributePaths = {"period", "period.house", "chore", "assignee"})
	Optional<Assignment> findWithDetailsById(Long id);

}
