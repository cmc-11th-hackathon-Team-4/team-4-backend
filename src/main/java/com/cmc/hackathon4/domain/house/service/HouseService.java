package com.cmc.hackathon4.domain.house.service;

import java.security.SecureRandom;

import com.cmc.hackathon4.domain.house.converter.HouseConverter;
import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
