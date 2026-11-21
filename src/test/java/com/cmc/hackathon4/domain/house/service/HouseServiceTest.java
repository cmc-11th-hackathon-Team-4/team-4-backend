package com.cmc.hackathon4.domain.house.service;

import com.cmc.hackathon4.domain.house.dto.HouseCreateRequest;
import com.cmc.hackathon4.domain.house.dto.HouseCreateResponse;
import com.cmc.hackathon4.domain.house.dto.HouseJoinRequest;
import com.cmc.hackathon4.domain.house.entity.House;
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
	void 다섯번째_참여는_정원초과로_막힌다() {
		User owner = newUser("dev-owner");
		HouseCreateResponse house = houseService.create(owner.getId(), new HouseCreateRequest("우리집"));
		for (int i = 1; i <= 3; i++) {
			User member = newUser("dev-m" + i);
			houseService.join(member.getId(), new HouseJoinRequest(house.inviteCode()));
		}
		User fifth = newUser("dev-m4");

		assertError(() -> houseService.join(fifth.getId(), new HouseJoinRequest(house.inviteCode())),
				ErrorCode.HOUSE_FULL);
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
