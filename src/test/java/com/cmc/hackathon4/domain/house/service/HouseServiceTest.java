package com.cmc.hackathon4.domain.house.service;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseInvitePreviewResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.entity.House;
import com.cmc.hackathon4.domain.house.entity.HouseStatus;
import com.cmc.hackathon4.domain.house.repository.HouseRepository;
import com.cmc.hackathon4.domain.user.entity.CharacterType;
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
class HouseServiceTest {

	@Autowired
	HouseService houseService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	HouseRepository houseRepository;

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
				.startDistributing(com.cmc.hackathon4.domain.house.entity.SettlementCycle.WEEKLY);
		houseRepository.flush();
		assertError(() -> houseService.getInvitePreview(distributing.inviteCode()), ErrorCode.HOUSE_DISTRIBUTING);
	}

	@Test
	void 배분중인_집에는_참여할_수_없다() {
		User owner = newUser("dev-owner");
		HouseCreateResponse created = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		House house = houseRepository.findById(created.id()).orElseThrow();
		house.startDistributing(com.cmc.hackathon4.domain.house.entity.SettlementCycle.WEEKLY);
		houseRepository.flush();
		User newcomer = newUser("dev-new");

		assertError(() -> houseService.join(newcomer.getId(), new HouseJoinRequest(created.inviteCode())),
				ErrorCode.HOUSE_DISTRIBUTING);
	}

}
