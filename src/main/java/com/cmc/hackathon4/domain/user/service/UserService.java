package com.cmc.hackathon4.domain.user.service;

import com.cmc.hackathon4.domain.user.converter.UserConverter;
import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserResponse;
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

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		if (userRepository.existsByDeviceId(request.deviceId())) {
			throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
		}
		try {
			return UserConverter.toResponse(userRepository.saveAndFlush(UserConverter.toEntity(request)));
		}
		catch (DataIntegrityViolationException e) {
			// 동시에 같은 deviceId 로 들어온 요청이 unique 제약에 걸린 경우
			throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
		}
	}

}
