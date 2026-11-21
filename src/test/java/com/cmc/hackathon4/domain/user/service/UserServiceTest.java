package com.cmc.hackathon4.domain.user.service;

import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseMember;
import com.cmc.hackathon4.domain.house.entity.MemberRole;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.entity.Notification;
import com.cmc.hackathon4.domain.notification.entity.NotificationType;
import com.cmc.hackathon4.domain.notification.repository.NotificationRepository;
import com.cmc.hackathon4.domain.user.dto.UserCreateRequest;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
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
	void 같은_deviceId_로_다시_등록하면_USER_409_예외가_난다() {
		UserCreateRequest request = new UserCreateRequest("device-dup", "수아", CharacterType.CHARACTER_1);
		userService.create(request);

		assertThatThrownBy(() -> userService.create(request)).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_ALREADY_REGISTERED);
	}

	@Test
	void 기분이_NO_CHOICE_로_바뀌면_같은_집_나머지_구성원에게만_알림이_생긴다() {
		User me = userRepository.save(new User("device-me", "수아", CharacterType.CHARACTER_1));
		User mate = userRepository.save(new User("device-mate", "민지", CharacterType.CHARACTER_2));
		House house = houseRepository.save(new House("302호", "K7Q2M9"));
		houseMemberRepository.save(new HouseMember(house, me, MemberRole.OWNER));
		houseMemberRepository.save(new HouseMember(house, mate, MemberRole.MEMBER));

		userService.updateMood(me.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));

		assertThat(notificationRepository.findAllByReceiverIdAndReadAtIsNullOrderByIdDesc(mate.getId()))
				.singleElement()
				.extracting(Notification::getType)
				.isEqualTo(NotificationType.MOOD_NO_CHOICE);
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
