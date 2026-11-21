package com.cmc.hackathon4.domain.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** EventSource 는 Accept: text/event-stream 을 보내므로, 연결 전 에러가 그때도 일반 JSON 으로 내려오는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationStreamErrorTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void deviceId가_없으면_400_COMMON_400_JSON이다() throws Exception {
		mockMvc.perform(get("/api/v1/notifications/stream").accept(MediaType.TEXT_EVENT_STREAM))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-400"));
	}

	@Test
	void 등록되지_않은_deviceId면_404_USER_404_JSON이다() throws Exception {
		mockMvc.perform(get("/api/v1/notifications/stream").param("deviceId", "unknown").accept(MediaType.TEXT_EVENT_STREAM))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER-404"));
	}

}
