package com.cmc.hackathon4.domain.house.entity;

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

@Getter
@Entity
@Table(name = "house", uniqueConstraints = @UniqueConstraint(name = "uk_house_invite_code", columnNames = "invite_code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class House extends BaseTimeEntity {

	public static final int MAX_MEMBERS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 15)
	private String name;

	@Column(name = "invite_code", nullable = false, length = 6)
	private String inviteCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private HouseStatus status;

	// 배분을 만들 때(#15) 저장된다. 그 전에는 null.
	@Enumerated(EnumType.STRING)
	@Column(name = "settlement_cycle", length = 20)
	private SettlementCycle settlementCycle;

	public House(String name, String inviteCode) {
		this.name = name;
		this.inviteCode = inviteCode;
		this.status = HouseStatus.WAITING;
	}

	public void rename(String name) {
		this.name = name;
	}

	public void changeSettlementCycle(SettlementCycle settlementCycle) {
		this.settlementCycle = settlementCycle;
	}

	/** WAITING → DISTRIBUTING. 이미 DISTRIBUTING 이면 주기만 바뀐다. */
	public void startDistributing(SettlementCycle settlementCycle) {
		this.settlementCycle = settlementCycle;
		this.status = HouseStatus.DISTRIBUTING;
	}

	/** DISTRIBUTING → ACTIVE */
	public void activate() {
		this.status = HouseStatus.ACTIVE;
	}

}
