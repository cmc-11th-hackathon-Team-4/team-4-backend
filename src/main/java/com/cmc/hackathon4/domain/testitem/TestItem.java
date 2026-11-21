package com.cmc.hackathon4.domain.testitem;

import com.cmc.hackathon4.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** [TEST] 배포·DB 연결 확인용 임시 테이블. 실제 기능이 생기면 도메인째 삭제한다. */
@Getter
@Entity
@Table(name = "test_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 1000)
	private String content;

	public TestItem(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

}
