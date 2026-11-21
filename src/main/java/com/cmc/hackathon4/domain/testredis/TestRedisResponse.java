package com.cmc.hackathon4.domain.testredis;

/** ttlSeconds: 남은 만료 시간(초). 만료 없음이면 -1 */
public record TestRedisResponse(String key, String value, Long ttlSeconds) {
}
