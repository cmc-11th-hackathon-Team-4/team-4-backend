package com.cmc.hackathon4.domain.notification.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 SSE 연결 목록. 사용자당 연결 1개이며 서버 메모리(ConcurrentHashMap)에만 둔다.
 * 전송이 실패한(끊긴) 연결은 예외를 삼키고 목록에서 지운다. 30초마다 ping 주석을 보내 연결을 유지한다.
 */
@Slf4j
@Component
public class NotificationEmitterRegistry {

	static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);
	static final long PING_INTERVAL_SECONDS = 30;

	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
	private ScheduledExecutorService pingScheduler;

	@PostConstruct
	void startPing() {
		pingScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "notification-sse-ping");
			thread.setDaemon(true);
			return thread;
		});
		pingScheduler.scheduleWithFixedDelay(this::pingAll, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS,
				TimeUnit.SECONDS);
	}

	@PreDestroy
	void shutdown() {
		if (pingScheduler != null) {
			pingScheduler.shutdownNow();
		}
		emitters.values().forEach(this::completeQuietly);
		emitters.clear();
	}

	/** 새 연결을 만들어 등록한다. 같은 사용자의 이전 연결은 종료한다. */
	public SseEmitter register(Long userId) {
		return register(userId, new SseEmitter(TIMEOUT_MILLIS));
	}

	/** 이미 만든 emitter 를 등록한다(테스트에서 가짜 emitter 를 넣을 때도 쓴다). */
	public SseEmitter register(Long userId, SseEmitter emitter) {
		emitter.onCompletion(() -> emitters.remove(userId, emitter));
		emitter.onTimeout(() -> emitters.remove(userId, emitter));
		emitter.onError(e -> emitters.remove(userId, emitter));
		SseEmitter previous = emitters.put(userId, emitter);
		if (previous != null && previous != emitter) {
			completeQuietly(previous);
		}
		return emitter;
	}

	public boolean hasConnection(Long userId) {
		return emitters.containsKey(userId);
	}

	/** 연결 직후 신호: event: connected / data: {} */
	public void sendConnected(Long userId, SseEmitter emitter) {
		sendTo(userId, emitter, SseEmitter.event().name("connected").data(Map.of(), MediaType.APPLICATION_JSON));
	}

	/** 사용자의 현재 연결로 알림을 보낸다. 연결이 없으면 아무것도 하지 않는다. */
	public void sendNotification(Long userId, NotificationResponse notification) {
		SseEmitter emitter = emitters.get(userId);
		if (emitter != null) {
			sendNotification(userId, emitter, notification);
		}
	}

	public void sendNotification(Long userId, SseEmitter emitter, NotificationResponse notification) {
		sendTo(userId, emitter, SseEmitter.event()
				.id(String.valueOf(notification.id()))
				.name("notification")
				.data(notification, MediaType.APPLICATION_JSON));
	}

	void pingAll() {
		try {
			emitters.forEach((userId, emitter) -> sendTo(userId, emitter, SseEmitter.event().comment("ping")));
		}
		catch (RuntimeException e) {
			// 스케줄러가 멈추지 않게 한다.
			log.warn("[NotificationSse] ping 실패: cause={}", e.getClass().getSimpleName());
		}
	}

	private void sendTo(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		}
		catch (Exception e) {
			// 끊긴 연결: 삼키고 목록에서 지운다. 알림 저장 트랜잭션에는 영향을 주지 않는다.
			emitters.remove(userId, emitter);
			completeQuietly(emitter);
		}
	}

	private void completeQuietly(SseEmitter emitter) {
		try {
			emitter.complete();
		}
		catch (Exception ignored) {
			// 이미 끊긴 연결
		}
	}

}
