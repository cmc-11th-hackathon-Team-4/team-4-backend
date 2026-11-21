package com.cmc.hackathon4.domain.user.service;

import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.dto.UserUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	UserService userService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;
	@Autowired
	HouseMemberRepository houseMemberRepository;
	@Autowired
	NotificationRepository notificationRepository;

	@Test
	void deviceId_는_해시로만_저장되고_원본으로_조회된다() {
		userService.create(new UserCreateRequest("device-plain", "수아", CharacterType.CHARACTER_1));

		User saved = userRepository.findByDeviceId("device-plain").orElseThrow();

		// SHA-256("device-plain") 의 소문자 hex. MySQL 의 SHA2('device-plain', 256) 과 같은 값이어야 한다.
		assertThat(saved.getDeviceIdHash()).hasSize(64).doesNotContain("device-plain").matches("[0-9a-f]{64}");
		assertThat(userRepository.findByDeviceIdHash("device-plain")).isEmpty();
		// 저장된 해시를 헤더 값으로 보내도 그 사용자로 식별되지 않는다.
		assertThat(userRepository.findByDeviceId(saved.getDeviceIdHash())).isEmpty();
	}

	@Test
	void 같은_deviceId_로_다시_등록하면_USER_409_예외가_난다() {
		UserCreateRequest request = new UserCreateRequest("device-dup", "수아", CharacterType.CHARACTER_1);
		userService.create(request);

		assertThatThrownBy(() -> userService.create(request)).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_ALREADY_REGISTERED);
	}

	@Test
	void 프로필은_이름과_캐릭터_중_보낸_필드만_바꾼다() {
		User user = userRepository.save(new User("device-update", "수아", CharacterType.CHARACTER_1));

		assertThat(userService.update(user.getId(), new UserUpdateRequest("민지", null)).characterType())
				.isEqualTo(CharacterType.CHARACTER_1);
		assertThat(userService.update(user.getId(), new UserUpdateRequest(null, CharacterType.CHARACTER_2)).name())
				.isEqualTo("민지");

		User saved = userRepository.findByDeviceId("device-update").orElseThrow();
		assertThat(saved.getName()).isEqualTo("민지");
		assertThat(saved.getCharacterType()).isEqualTo(CharacterType.CHARACTER_2);
	}

	@Test
	void 내_프로필은_집에_가입한_후에만_소속_집과_역할을_담는다() {
		User user = userRepository.save(new User("device-get-me", "수아", CharacterType.CHARACTER_1));
		assertThat(userService.getMe(user.getId()).house()).isNull();

		House house = houseRepository.save(new House("302호", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, user, MemberRole.OWNER));

		assertThat(userService.getMe(user.getId()).house()).satisfies(membership -> {
			assertThat(membership.id()).isEqualTo(house.getId());
			assertThat(membership.role()).isEqualTo(MemberRole.OWNER);
		});
	}

	@ParameterizedTest
	@CsvSource({"수아, 수아가", "수빈, 수빈이", "Alex, Alex가", "별🌟, 별🌟가"})
	void 기분이_NO_CHOICE_로_바뀌면_같은_집_나머지_구성원에게만_알림이_생긴다(String name, String subject) {
		User me = userRepository.save(new User("device-me", name, CharacterType.CHARACTER_1));
		User mate = userRepository.save(new User("device-mate", "민지", CharacterType.CHARACTER_2));
		House house = houseRepository.save(new House("302호", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, me, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, mate, MemberRole.MEMBER));

		userService.updateMood(me.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));

		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(mate.getId()))
				.singleElement()
				.satisfies(notification -> {
					assertThat(notification.getType()).isEqualTo(NotificationType.MOOD_NO_CHOICE);
					assertThat(notification.getMessage()).isEqualTo(subject + " 아무것도 고르기 싫대요");
				});
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(me.getId())).isEmpty();
	}

	@Test
	void 이미_NO_CHOICE_였거나_NO_CHOICE_가_아니면_알림이_없고_집이_없어도_알림이_없다() {
		User me = userRepository.save(new User("device-me2", "수아", CharacterType.CHARACTER_1));
		User mate = userRepository.save(new User("device-mate2", "민지", CharacterType.CHARACTER_2));
		House house = houseRepository.save(new House("302호", "K7Q2M8"));
		houseMemberRepository.save(new HouseMember(house, me, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, mate, MemberRole.MEMBER));
		User loner = userRepository.save(new User("device-loner", "혼자", CharacterType.CHARACTER_3));

		userService.updateMood(me.getId(), new UserMoodUpdateRequest(Mood.TIRED));
		userService.updateMood(me.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));
		userService.updateMood(me.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));
		userService.updateMood(loner.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));

		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(mate.getId())).hasSize(1);
		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(loner.getId())).isEmpty();
	}

}
