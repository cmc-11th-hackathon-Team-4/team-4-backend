package com.cmc.hackathon4.domain.house.service;

import java.time.LocalDate;

import com.cmc.hackathon4.domain.house.dto.HomeResponse;
import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseInvitePreviewResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.dto.HouseUpdateRequest;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseMemberRepository;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.notification.service.NotificationService;
import com.cmc.hackathon4.domain.period.entity.Period;
import com.cmc.hackathon4.domain.period.repository.PeriodRepository;
import com.cmc.hackathon4.domain.user.dto.UserMoodUpdateRequest;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
import com.cmc.hackathon4.domain.user.entity.Mood;
import com.cmc.hackathon4.domain.user.entity.User;
import com.cmc.hackathon4.domain.user.repository.UserRepository;
import com.cmc.hackathon4.domain.user.service.UserService;
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
class HouseServiceTest {

	@Autowired
	HouseService houseService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;
	@Autowired
	HouseMemberRepository houseMemberRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	HouseQueryService houseQueryService;
	@Autowired
	NotificationService notificationService;
	@Autowired
	UserService userService;

	private User newUser(String deviceId) {
		return userRepository.save(new User(deviceId, "이름", CharacterType.CHARACTER_1));
	}

	private void assertError(Runnable action, ErrorCode expected) {
		assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(expected);
	}

	@Test
	void 집을_만들면_영대문자_숫자_6자리_초대코드가_생긴다() {
		User owner = newUser("dev-owner");

		HouseCreateResponse response = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));

		assertThat(response.inviteCode()).matches("^[A-Z0-9]{6}$");
		assertThat(response.status().name()).isEqualTo("WAITING");
	}

	@Test
	void 이미_집이_있으면_만들기와_참여가_막힌다() {
		User owner = newUser("dev-owner");
		User other = newUser("dev-other");
		HouseCreateResponse house = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		houseService.create(other.getId(), new HouseCreateRequest("다른집"));

		assertError(() -> houseService.create(owner.getId(), new HouseCreateRequest("또집")),
				ErrorCode.HOUSE_ALREADY_JOINED);
		assertError(() -> houseService.join(other.getId(), new HouseJoinRequest(house.inviteCode())),
				ErrorCode.HOUSE_ALREADY_JOINED);
	}

	@Test
	void 정원_5명이_차면_여섯번째_참여는_정원초과로_막힌다() {
		User owner = newUser("dev-owner");
		HouseCreateResponse house = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		for (int i = 1; i <= 4; i++) {
			User member = newUser("dev-m" + i);
			houseService.join(member.getId(), new HouseJoinRequest(house.inviteCode()));
		}
		User sixth = newUser("dev-m5");

		assertError(() -> houseService.join(sixth.getId(), new HouseJoinRequest(house.inviteCode())),
				ErrorCode.HOUSE_FULL);
	}

	@Test
	void 초대_코드로_집을_미리보면_이름_인원_정원_방장_상태가_나온다() {
		User owner = userRepository.save(new User("dev-owner", "방장", CharacterType.CHARACTER_1));
		HouseCreateResponse house = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		houseService.join(newUser("dev-m1").getId(), new HouseJoinRequest(house.inviteCode()));

		HouseInvitePreviewResponse preview = houseService.getInvitePreview(house.inviteCode());

		assertThat(preview.name()).isEqualTo("우리집");
		assertThat(preview.memberCount()).isEqualTo(2);
		assertThat(preview.maxMemberCount()).isEqualTo(House.MAX_MEMBERS);
		assertThat(preview.ownerName()).isEqualTo("방장");
		assertThat(preview.status()).isEqualTo(HouseStatus.WAITING);
	}

	@Test
	void 미리보기는_참여할_수_없는_집이면_참여와_같은_에러를_낸다() {
		User owner = newUser("dev-owner");
		HouseCreateResponse created = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		for (int i = 1; i <= 4; i++) {
			houseService.join(newUser("dev-m" + i).getId(), new HouseJoinRequest(created.inviteCode()));
		}
		assertError(() -> houseService.getInvitePreview(created.inviteCode()), ErrorCode.HOUSE_FULL);
		assertError(() -> houseService.getInvitePreview("ZZZZZZ"), ErrorCode.HOUSE_INVALID_INVITE_CODE);
		assertError(() -> houseService.getInvitePreview("abc123"), ErrorCode.INVALID_INPUT);

		User other = newUser("dev-other");
		HouseCreateResponse distributing = houseService.create(other.getId(), new HouseCreateRequest("배분중"));
		houseRepository.findById(distributing.id()).orElseThrow()
				.startDistributing(7);
		houseRepository.flush();
		assertError(() -> houseService.getInvitePreview(distributing.inviteCode()), ErrorCode.HOUSE_DISTRIBUTING);
	}

	@Test
	void 배분중인_집에는_참여할_수_없다() {
		User owner = newUser("dev-owner");
		HouseCreateResponse created = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		House house = houseRepository.findById(created.id()).orElseThrow();
		house.startDistributing(7);
		houseRepository.flush();
		User newcomer = newUser("dev-new");

		assertError(() -> houseService.join(newcomer.getId(), new HouseJoinRequest(created.inviteCode())),
				ErrorCode.HOUSE_DISTRIBUTING);
	}

	@Test
	void 대기_중에는_방장만_다른_구성원을_내보낼_수_있다() {
		User owner = newUser("dev-owner");
		User member = newUser("dev-member");
		User outsider = newUser("dev-outsider");
		HouseCreateResponse house = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		houseService.join(member.getId(), new HouseJoinRequest(house.inviteCode()));

		assertError(() -> houseService.removeMember(house.id(), member.getId(), owner.getId()),
				ErrorCode.HOUSE_OWNER_ONLY);
		assertError(() -> houseService.removeMember(house.id(), owner.getId(), owner.getId()), ErrorCode.INVALID_INPUT);
		assertError(() -> houseService.removeMember(house.id(), owner.getId(), outsider.getId()),
				ErrorCode.INVALID_INPUT);
		houseService.removeMember(house.id(), owner.getId(), member.getId());

		assertThat(houseMemberRepository.findByUserId(member.getId())).isEmpty();
		assertThat(houseMemberRepository.findByUserId(owner.getId())).isPresent();
	}

	@Test
	void 내보내진_구성원은_새_집에서_이전_집의_알림을_보지_않는다() {
		User oldOwner = newUser("dev-old-owner");
		User member = newUser("dev-member");
		HouseCreateResponse oldHouse = houseService.create(oldOwner.getId(), new HouseCreateRequest("이전집"));
		houseService.join(member.getId(), new HouseJoinRequest(oldHouse.inviteCode()));
		// 이전 집 방장이 '아무것도 고르기 싫어요' 로 바꾸면 member 에게 MOOD_NO_CHOICE 알림이 간다
		userService.updateMood(oldOwner.getId(), new UserMoodUpdateRequest(Mood.NO_CHOICE));
		assertThat(notificationService.findMine(member.getId(), true, null)).hasSize(1);

		houseService.removeMember(oldHouse.id(), oldOwner.getId(), member.getId());
		User newOwner = newUser("dev-new-owner");
		HouseCreateResponse newHouse = houseService.create(newOwner.getId(), new HouseCreateRequest("새집"));
		houseService.join(member.getId(), new HouseJoinRequest(newHouse.inviteCode()));

		HomeResponse home = houseQueryService.getHome(newHouse.id(), member.getId());
		assertThat(home.notifications()).isEmpty();
		assertThat(notificationService.findMine(member.getId(), false, null)).isEmpty();
		assertThat(notificationService.findMissed(member.getId(), 0L)).isEmpty();
	}

	@Test
	void 배분_중이거나_시작한_집에서는_구성원을_내보낼_수_없다() {
		User owner = newUser("dev-owner");
		User member = newUser("dev-member");
		HouseCreateResponse created = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		houseService.join(member.getId(), new HouseJoinRequest(created.inviteCode()));
		House house = houseRepository.findById(created.id()).orElseThrow();

		house.startDistributing(7);
		assertError(() -> houseService.removeMember(house.getId(), owner.getId(), member.getId()),
				ErrorCode.HOUSE_DISTRIBUTING);
		house.activate();
		assertError(() -> houseService.removeMember(house.getId(), owner.getId(), member.getId()),
				ErrorCode.HOUSE_ALREADY_STARTED);

		assertThat(houseMemberRepository.findByHouseIdAndUserId(house.getId(), member.getId())).isPresent();
	}

	@Test
	void 집_상세는_상태에_맞는_기간을_반환한다() {
		User owner = newUser("dev-owner");
		Long houseId = houseService.create(owner.getId(), new HouseCreateRequest("우리집")).id();
		House house = houseRepository.findById(houseId).orElseThrow();
		LocalDate start = LocalDate.of(2026, 11, 23);

		assertThat(houseService.getDetail(houseId, owner.getId()).currentPeriod()).isNull();

		house.startDistributing(7);
		Period first = periodRepository.save(new Period(house, 1, start, start.plusDays(6)));
		assertThat(houseService.getDetail(houseId, owner.getId()).currentPeriod().id()).isEqualTo(first.getId());

		house.activate();
		first.start();
		LocalDate nextStart = start.plusWeeks(1);
		periodRepository.save(new Period(house, 2, nextStart, nextStart.plusDays(6)));
		assertThat(houseService.getDetail(houseId, owner.getId()).currentPeriod().id()).isEqualTo(first.getId());
	}

	@Test
	void 집_이름과_결산_주기는_보낸_필드만_수정하고_진행_중_기간은_유지한다() {
		User owner = newUser("dev-owner");
		Long houseId = houseService.create(owner.getId(), new HouseCreateRequest("우리집")).id();
		House house = houseRepository.findById(houseId).orElseThrow();
		LocalDate start = LocalDate.of(2026, 11, 23);
		house.startDistributing(7);
		house.activate();
		Period current = periodRepository.save(new Period(house, 1, start, start.plusDays(6)));
		current.start();

		assertThat(houseService.update(houseId, owner.getId(), new HouseUpdateRequest(" 새이름 ", null))
				.settlementCycle()).isEqualTo(7);
		assertThat(houseService.update(houseId, owner.getId(), new HouseUpdateRequest(null, 3))
				.name()).isEqualTo("새이름");

		assertThat(houseService.getDetail(houseId, owner.getId()).settlementCycle()).isEqualTo(3);
		assertThat(houseService.getDetail(houseId, owner.getId()).currentPeriod().endDate()).isEqualTo(start.plusDays(6));
	}

}
