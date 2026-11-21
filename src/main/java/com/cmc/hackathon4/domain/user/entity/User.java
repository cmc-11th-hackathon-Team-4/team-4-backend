package com.cmc.hackathon4.domain.user.entity;

import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인이 없어 기기 UUID(deviceId)로 식별하는 사용자. `user` 는 예약어라 테이블 이름은 users.
 * deviceId 는 그 값만 알면 본인으로 요청할 수 있는 값이라 평문 대신 해시(device_id 컬럼)만 저장한다.
 */
@Getter
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_device_id", columnNames = "device_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "device_id", nullable = false, length = 64)
	private String deviceIdHash;

	@Column(nullable = false, length = 10)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "character_type", nullable = false, length = 20)
	private CharacterType characterType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Mood mood;

	/** @param deviceId 앱이 보낸 기기 UUID 원본. 해시해서 저장한다. */
	public User(String deviceId, String name, CharacterType characterType) {
		this.deviceIdHash = DeviceIdHasher.hash(deviceId);
		this.name = name;
		this.characterType = characterType;
		this.mood = Mood.RELAXED;
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changeCharacterType(CharacterType characterType) {
		this.characterType = characterType;
	}

	public void changeMood(Mood mood) {
		this.mood = mood;
	}

}
