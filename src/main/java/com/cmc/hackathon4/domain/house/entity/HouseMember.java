package com.cmc.hackathon4.domain.house.entity;

import com.cmc.hackathon4.domain.user.entity.User;

import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 집 구성원. 한 사용자는 한 집에만 속한다(user_id UNIQUE). 정원(House.MAX_MEMBERS)은 서비스에서 검사한다. */
@Getter
@Entity
@Table(name = "house_member", uniqueConstraints = @UniqueConstraint(name = "uk_house_member_user", columnNames = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "house_id", nullable = false)
	private House house;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberRole role;

	public HouseMember(House house, User user, MemberRole role) {
		this.house = house;
		this.user = user;
		this.role = role;
	}

	public boolean isOwner() {
		return role == MemberRole.OWNER;
	}

}
