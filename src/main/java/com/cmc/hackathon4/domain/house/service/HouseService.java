package com.cmc.hackathon4.domain.house.service;

import java.security.SecureRandom;
import java.util.List;

import com.cmc.hackathon4.domain.chore.repository.ChoreRepository;
import com.cmc.hackathon4.domain.house.converter.HouseConverter;
import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseDetailResponse;
import com.cmc.hackathon4.domain.house.dto.HouseInvitePreviewResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.dto.HouseJoinResponse;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.entity.PeriodStatus;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseService {

	private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int INVITE_CODE_LENGTH = 6;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final HouseRepository houseRepository;
	private final HouseMemberRepository houseMemberRepository;
	private final UserRepository userRepository;
	private final ChoreRepository choreRepository;
	private final PeriodRepository periodRepository;
	private final HouseMemberValidator houseMemberValidator;

	@Transactional
	public HouseCreateResponse create(Long userId, HouseCreateRequest request) {
		User user = findUser(userId);
		if (houseMemberRepository.existsByUserId(userId)) {
			throw new BusinessException(ErrorCode.HOUSE_ALREADY_JOINED);
		}
		House house = houseRepository.save(HouseConverter.toEntity(request, generateInviteCode()));
		houseMemberRepository.save(new HouseMember(house, user, MemberRole.OWNER));
		return HouseConverter.toCreateResponse(house);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public HouseJoinResponse join(Long userId, HouseJoinRequest request) {
		User user = findUser(userId);
		if (houseMemberRepository.existsByUserId(userId)) {
			throw new BusinessException(ErrorCode.HOUSE_ALREADY_JOINED);
		}
		// 동시 참여로 정원을 넘지 않도록 집 행을 잠근 뒤 상태와 인원을 확인한다. 잠금을 기다린 뒤의 조회가 앞선 참여를 보도록 READ_COMMITTED 로 돈다.
		House house = houseRepository.findByInviteCodeForUpdate(request.inviteCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_INVALID_INVITE_CODE));
		validateJoinable(house, houseMemberRepository.countByHouseId(house.getId()));
		houseMemberRepository.save(new HouseMember(house, user, MemberRole.MEMBER));
		return HouseConverter.toJoinResponse(house);
	}

	/** S-13-C 참여 전 미리보기. 로그인 없이 초대 코드로 조회하며, 참여할 수 없는 집이면 join 과 같은 에러를 낸다. */
	public HouseInvitePreviewResponse getInvitePreview(String inviteCode) {
		// 경로 변수 제약(@Pattern)을 컨트롤러에 달면 XxxApiDocs 인터페이스와 충돌(HV000151)해서 여기서 검사한다
		if (inviteCode == null || !inviteCode.matches("^[A-Z0-9]{6}$")) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "inviteCode: 초대 코드는 영대문자와 숫자 6자리여야 합니다.");
		}
		House house = houseRepository.findByInviteCode(inviteCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_INVALID_INVITE_CODE));
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(house.getId());
		validateJoinable(house, members.size());
		return HouseConverter.toInvitePreviewResponse(house, members);
	}

	// join 과 미리보기가 같은 기준을 쓰도록 한 곳에 둔다
	private void validateJoinable(House house, long memberCount) {
		if (house.getStatus() == HouseStatus.DISTRIBUTING) {
			throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
		}
		if (memberCount >= House.MAX_MEMBERS) {
			throw new BusinessException(ErrorCode.HOUSE_FULL);
		}
	}

	public HouseDetailResponse getDetail(Long houseId, Long userId) {
		houseMemberValidator.getMember(houseId, userId);
		House house = findHouse(houseId);
		List<HouseMember> members = houseMemberRepository.findAllByHouseIdOrderByIdAsc(houseId);
		int choreCount = choreRepository.findAllByHouseIdAndActiveTrueOrderByIdAsc(houseId).size();
		return HouseConverter.toDetailResponse(house, members, userId, choreCount, findCurrentPeriod(house));
	}

	/** 보낸 필드만 수정한다. 주기는 다음 기간부터 적용되므로 진행 중 기간은 건드리지 않는다. */
	@Transactional
	public HouseUpdateResponse update(Long houseId, Long userId, HouseUpdateRequest request) {
		houseMemberValidator.getOwner(houseId, userId);
		House house = findHouse(houseId);
		if (request.name() != null) {
			house.rename(request.name().trim());
		}
		if (request.settlementCycle() != null) {
			house.changeSettlementCycle(request.settlementCycle());
		}
		return HouseConverter.toUpdateResponse(house);
	}

	/** 방장이 WAITING 상태의 집에서 구성원을 내보낸다. */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeMember(Long houseId, Long ownerUserId, Long targetUserId) {
		houseMemberValidator.getOwner(houseId, ownerUserId);
		// 동시 참여와 겹치지 않도록 집 행을 잠근다.
		House house = houseRepository.findByIdForUpdate(houseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
		switch (house.getStatus()) {
			case DISTRIBUTING -> throw new BusinessException(ErrorCode.HOUSE_DISTRIBUTING);
			case ACTIVE -> throw new BusinessException(ErrorCode.HOUSE_ALREADY_STARTED);
			case WAITING -> {
			}
		}
		if (ownerUserId.equals(targetUserId)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		HouseMember target = houseMemberRepository.findByHouseIdAndUserId(houseId, targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
		houseMemberRepository.delete(target);
	}

	/** WAITING 은 null, DISTRIBUTING 은 READY 기간, ACTIVE 는 READY 를 뺀 최신 기간. */
	private Period findCurrentPeriod(House house) {
		return switch (house.getStatus()) {
			case WAITING -> null;
			case DISTRIBUTING -> periodRepository.findByHouseIdAndStatus(house.getId(), PeriodStatus.READY)
					.orElse(null);
			case ACTIVE -> periodRepository
					.findFirstByHouseIdAndStatusNotOrderByRoundDesc(house.getId(), PeriodStatus.READY)
					.orElse(null);
		};
	}

	private House findHouse(Long houseId) {
		return houseRepository.findById(houseId).orElseThrow(() -> new BusinessException(ErrorCode.HOUSE_NOT_FOUND));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private String generateInviteCode() {
		String code;
		do {
			StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
			for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
				sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
			}
			code = sb.toString();
		}
		while (houseRepository.existsByInviteCode(code));
		return code;
	}

}
