package com.cmc.hackathon4.domain.user.service;

import java.util.List;

import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.user.converter.UserConverter;
import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMeResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodResponse;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
import com.cmc.hackathon4.domain.user.dto.UserUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.Mood;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final HouseMemberRepository houseMemberRepository;
	private final NotificationService notificationService;

	public UserMeResponse getMe(Long userId) {
		User user = getUser(userId);
		HouseMember membership = houseMemberRepository.findByUserId(userId).orElse(null);
		return UserConverter.toMeResponse(user, membership);
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional
	public UserResponse update(Long userId, UserUpdateRequest request) {
		User user = getUser(userId);
		if (request.name() != null) {
			user.changeName(validName(request.name()));
		}
		if (request.characterType() != null) {
			user.changeCharacterType(request.characterType());
		}
		return UserConverter.toResponse(user);
	}

	/** 기분을 바꾼다. 이전 값이 NO_CHOICE 가 아니었다가 NO_CHOICE 가 될 때만 같은 집의 나머지 구성원에게 알린다. */
	@Transactional
	public UserMoodResponse updateMood(Long userId, UserMoodUpdateRequest request) {
		User user = getUser(userId);
		Mood previous = user.getMood();
		user.changeMood(request.mood());
		if (previous != Mood.NO_CHOICE && request.mood() == Mood.NO_CHOICE) {
			notifyNoChoice(user);
		}
		return UserConverter.toMoodResponse(user);
	}

	private void notifyNoChoice(User user) {
		houseMemberRepository.findByUserId(user.getId()).ifPresent(membership -> {
			List<User> receivers = houseMemberRepository.findAllByHouseIdOrderByIdAsc(membership.getHouse().getId())
					.stream()
					.map(HouseMember::getUser)
					.filter(member -> !member.getId().equals(user.getId()))
					.toList();
			notificationService.sendAll(receivers, user, null, NotificationType.MOOD_NO_CHOICE,
					user.getName() + subjectParticle(user.getName()) + " 아무것도 고르기 싫대요");
		});
	}

	/** 마지막 글자 받침 여부에 따라 주격 조사(이/가)를 고른다. 한글이 아니면 '가'. */
	private static String subjectParticle(String name) {
		char last = name.charAt(name.length() - 1);
		boolean hangul = last >= '가' && last <= '힣';
		return hangul && (last - '가') % 28 != 0 ? "이" : "가";
	}

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		String name = validName(request.name());
		if (userRepository.existsByDeviceId(request.deviceId())) {
			throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
		}
		try {
			return UserConverter.toResponse(userRepository.saveAndFlush(UserConverter.toEntity(request, name)));
		}
		catch (DataIntegrityViolationException e) {
			// 동시에 같은 deviceId 로 들어온 요청이 unique 제약에 걸린 경우
			throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
		}
	}

	/**
	 * 앞뒤 공백(전각 공백 포함)을 뺀 이름을 돌려준다. 길이는 이모지도 한 글자로 세도록 코드 포인트로 센다. @Size 는 UTF-16 단위로 세서 이모지 6개도 10자를
	 * 넘는 것으로 보기 때문에 요청 record 대신 여기서 검사한다.
	 */
	private static String validName(String name) {
		String stripped = name.strip();
		if (stripped.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "name: 이름은 공백만 입력할 수 없습니다.");
		}
		if (stripped.codePointCount(0, stripped.length()) > User.NAME_MAX_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "name: 이름은 1~10자여야 합니다.");
		}
		return stripped;
	}

}
