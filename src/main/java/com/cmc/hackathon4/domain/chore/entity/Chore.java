package com.cmc.hackathon4.domain.chore.entity;

import com.cmc.hackathon4.domain.house.entity.House;

import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chore")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chore extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "house_id", nullable = false)
	private House house;

	@Column(nullable = false, length = 20)
	private String name;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	public Chore(House house, String name) {
		this.house = house;
		this.name = name;
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

}
