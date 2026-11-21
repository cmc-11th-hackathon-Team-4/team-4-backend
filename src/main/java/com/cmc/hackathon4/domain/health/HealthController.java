package com.cmc.hackathon4.domain.health;

import java.time.Instant;

import com.cmc.hackathon4.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController implements HealthApiDocs {

	@Override
	@GetMapping
	public ApiResponse<HealthResponse> health() {
		return ApiResponse.ok(new HealthResponse("UP", Instant.now()));
	}

}
