package com.cmc.hackathon4.domain.testredis;

import java.time.Duration;

import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** [TEST] Redis 연결 확인용. 다른 키와 섞이지 않게 "test:" 접두사를 붙여 저장한다. */
@Service
@RequiredArgsConstructor
public class TestRedisService {

	private static final String PREFIX = "test:";

	private final StringRedisTemplate redisTemplate;

	public TestRedisResponse save(String key, TestRedisSaveRequest request) {
		String redisKey = PREFIX + key;
		if (request.ttlSeconds() == null) {
			redisTemplate.opsForValue().set(redisKey, request.value());
		} else {
			redisTemplate.opsForValue().set(redisKey, request.value(), Duration.ofSeconds(request.ttlSeconds()));
		}
		return find(key);
	}

	public TestRedisResponse find(String key) {
		String redisKey = PREFIX + key;
		String value = redisTemplate.opsForValue().get(redisKey);
		if (value == null) {
			throw new BusinessException(ErrorCode.TEST_REDIS_KEY_NOT_FOUND);
		}
		return new TestRedisResponse(key, value, redisTemplate.getExpire(redisKey));
	}

	public void delete(String key) {
		if (!Boolean.TRUE.equals(redisTemplate.delete(PREFIX + key))) {
			throw new BusinessException(ErrorCode.TEST_REDIS_KEY_NOT_FOUND);
		}
	}

}
