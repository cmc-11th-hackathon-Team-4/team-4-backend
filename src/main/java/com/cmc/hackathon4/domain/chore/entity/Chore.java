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
import org.hibernate.annotations.ColumnDefault;

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

	// 시작한 뒤 추가된, 그 기간(주)에만 유효한 집안일. 기간이 끝나면 비활성화된다.
	// 이미 행이 있는 테이블에 추가되는 컬럼이라 기본값을 둔다 (기존 집안일은 false)
	@ColumnDefault("false")
	@Column(name = "is_temporary", nullable = false)
	private boolean temporary;

	public Chore(House house, String name) {
		this(house, name, false);
	}

	public Chore(House house, String name, boolean temporary) {
		this.house = house;
		this.name = name;
		this.active = true;
		this.temporary = temporary;
	}

	public void deactivate() {
		this.active = false;
	}

}
