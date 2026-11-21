package com.cmc.hackathon4.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Regression: 4차 QA — 정수 필드에 3.5, "7" 을 보내면 3, 7 로 바꿔 받아 1/3/7 검증을 통과하던 문제
@SpringBootTest
class JacksonConfigTest {

	record Days(Integer days) {
	}

	@Autowired
	JsonMapper jsonMapper;

	@Test
	void 정수_필드는_정수만_받는다() {
		assertThat(jsonMapper.readValue("{\"days\":7}", Days.class).days()).isEqualTo(7);
	}

	@Test
	void 정수_필드에_소수가_오면_거절한다() {
		assertThatThrownBy(() -> jsonMapper.readValue("{\"days\":3.5}", Days.class))
				.isInstanceOf(Exception.class);
	}

	@Test
	void 정수_필드에_문자열이_오면_거절한다() {
		assertThatThrownBy(() -> jsonMapper.readValue("{\"days\":\"7\"}", Days.class))
				.isInstanceOf(Exception.class);
	}

}
