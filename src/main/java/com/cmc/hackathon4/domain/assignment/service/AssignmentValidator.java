package com.cmc.hackathon4.domain.assignment.service;

import com.cmc.hackathon4.domain.assignment.entity.Assignment;
import com.cmc.hackathon4.domain.assignment.entity.AssignmentStatus;
import com.cmc.hackathon4.domain.assignment.repository.AssignmentRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 짐을 바꾸는 API(완료·덜어가기·부탁·되돌리기·집안일 추가)의 공통 상태 검사. */
@Component
@RequiredArgsConstructor
public class AssignmentValidator {

	/** 한 기간에서 한 사람이 가질 수 있는 남은(TODO) 짐 수 */
	public static final int MAX_TODO_PER_USER = 5;

	private final AssignmentRepository assignmentRepository;

	/** 아직 처리할 수 있는 짐인지 확인한다. 완료했으면 ASSIGNMENT-409, 그다음은 {@link #validatePeriodActive} 와 같다. */
	public void validateOpen(Assignment assignment) {
		if (assignment.isDone()) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_ALREADY_DONE);
		}
		validatePeriodActive(assignment.getPeriod());
	}

	/** 진행 중(ACTIVE) 기간인지 확인한다. 끝났으면 PERIOD_ENDED, 아직 시작 전이면 PERIOD_NOT_STARTED. */
	public void validatePeriodActive(Period period) {
		if (period.isEnded()) {
			throw new BusinessException(ErrorCode.PERIOD_ENDED);
		}
		if (!period.isActive()) {
			throw new BusinessException(ErrorCode.PERIOD_NOT_STARTED);
		}
	}

	/** 그 기간에서 사용자의 남은(TODO) 짐이 한도에 이르렀으면 막는다. 호출 전에 집 행을 이미 잠가 둔 상태여야 한다. */
	public void validateTodoLimit(Long periodId, Long userId) {
		if (assignmentRepository.countByPeriodIdAndAssigneeIdAndStatusAndDeletedAtIsNull(periodId, userId,
				AssignmentStatus.TODO) >= MAX_TODO_PER_USER) {
			throw new BusinessException(ErrorCode.ASSIGNMENT_LIMIT_EXCEEDED);
		}
	}

}
