package com.cmc.hackathon4.domain.notification.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cmc.hackathon4.domain.notification.dto.NotificationResponse;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커밋 뒤 전송은 실제 커밋이 필요해서 @Transactional 없이 TransactionTemplate 으로 커밋하고, 만든 데이터는 직접 지운다.
 * 실제 HTTP 연결 대신 전송 내용을 기록하는 가짜 emitter 를 등록한다.
 */
@SpringBootTest
class NotificationSseTest {

	@Autowired
	NotificationService notificationService;
	@Autowired
	NotificationEmitterRegistry registry;
	@Autowired
	NotificationRepository notificationRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	TransactionTemplate transactionTemplate;

	User receiver;
	String receiverDeviceId;

	@BeforeEach
	void setUp() {
		receiverDeviceId = UUID.randomUUID().toString();
		receiver = userRepository.save(new User(receiverDeviceId, "수아", CharacterType.CHARACTER_1));
	}

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void 커밋_뒤에_등록된_emitter로_알림이_전달된다() {
		RecordingEmitter emitter = new RecordingEmitter();
		registry.register(receiver.getId(), emitter);

		Long id = transactionTemplate.execute(status -> {
			Long saved = notificationService
					.send(userRepository.getReferenceById(receiver.getId()), null, null, NotificationType.PERIOD_STARTED,
							"새 집안일이 배분됐어요")
					.getId();
			// 커밋 전에는 아직 아무것도 전송되지 않는다.
			assertThat(emitter.events).isEmpty();
			return saved;
		});

		assertThat(emitter.events).anySatisfy(
				part -> assertThat(part).isEqualTo("id:" + id + "\nevent:notification\ndata:"));
		assertThat(emitter.events).anySatisfy(part -> assertThat(part).isInstanceOfSatisfying(NotificationResponse.class,
				response -> {
					assertThat(response.id()).isEqualTo(id);
					assertThat(response.message()).isEqualTo("새 집안일이 배분됐어요");
				}));
	}

	@Test
	void 롤백되면_전송되지_않는다() {
		RecordingEmitter emitter = new RecordingEmitter();
		registry.register(receiver.getId(), emitter);

		transactionTemplate.executeWithoutResult(status -> {
			notificationService.send(userRepository.getReferenceById(receiver.getId()), null, null,
					NotificationType.PERIOD_STARTED, "롤백될 알림");
			status.setRollbackOnly();
		});

		assertThat(emitter.events).isEmpty();
	}

	@Test
	void 전송이_실패하면_예외를_삼키고_연결을_지우며_알림은_저장된다() {
		RecordingEmitter emitter = new RecordingEmitter();
		emitter.failing = true;
		registry.register(receiver.getId(), emitter);

		transactionTemplate.executeWithoutResult(status -> notificationService.send(
				userRepository.getReferenceById(receiver.getId()), null, null, NotificationType.PERIOD_STARTED, "저장은 된다"));

		assertThat(registry.hasConnection(receiver.getId())).isFalse();
		assertThat(notificationRepository.count()).isEqualTo(1);
	}

	@Test
	void 새_연결이_오면_이전_연결은_종료되고_새_연결로만_전달된다() {
		RecordingEmitter first = new RecordingEmitter();
		RecordingEmitter second = new RecordingEmitter();
		registry.register(receiver.getId(), first);
		registry.register(receiver.getId(), second);

		transactionTemplate.executeWithoutResult(status -> notificationService.send(
				userRepository.getReferenceById(receiver.getId()), null, null, NotificationType.PERIOD_ENDED, "결산"));

		assertThat(first.events).isEmpty();
		assertThat(second.events).isNotEmpty();
	}

	@Test
	void 구독하면_연결이_등록되고_deviceId가_없거나_미등록이면_에러다() {
		Long n1 = transactionTemplate.execute(status -> notificationService
				.send(userRepository.getReferenceById(receiver.getId()), null, null, NotificationType.PERIOD_STARTED, "1")
				.getId());
		Long n2 = transactionTemplate.execute(status -> notificationService
				.send(userRepository.getReferenceById(receiver.getId()), null, null, NotificationType.PERIOD_EXTENDED, "2")
				.getId());
		NotificationStreamService streamService = new NotificationStreamService(userRepository, notificationService,
				registry);

		SseEmitter emitter = streamService.subscribe(receiverDeviceId, String.valueOf(n1));

		assertThat(emitter).isNotNull();
		assertThat(registry.hasConnection(receiver.getId())).isTrue();
		// 재전송 내용 자체는 실제 HTTP 연결 없이는 읽기 어려워 NotificationServiceTest.findMissed 로 확인한다.
		assertThat(n2).isGreaterThan(n1);
		assertThatThrownBy(() -> streamService.subscribe(" ", null)).isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
		assertThatThrownBy(() -> streamService.subscribe("unknown-device", null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
	}

	/** send 한 이벤트 조각(id:, event:, data 객체 등)을 기록한다. */
	static class RecordingEmitter extends SseEmitter {

		final List<Object> events = new ArrayList<>();
		boolean failing;

		@Override
		public void send(SseEventBuilder builder) throws IOException {
			if (failing) {
				throw new IOException("끊긴 연결");
			}
			builder.build().forEach(part -> events.add(part.getData()));
		}

	}

}
