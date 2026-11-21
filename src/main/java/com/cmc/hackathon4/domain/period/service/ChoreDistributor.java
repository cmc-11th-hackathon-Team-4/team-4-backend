package com.cmc.hackathon4.domain.period.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 집안일을 구성원에게 랜덤으로 나누는 로직. 온보딩 배분(#15·#36)과 다음 주기 배분(#32)이 같이 쓴다.
 * 이미 고정된 짐(이월된 짐)의 개수를 함께 받아, 총 개수가 균등해지도록 나머지를 나눈다.
 */
@Component
public class ChoreDistributor {

	private final Random random;

	@Autowired
	public ChoreDistributor() {
		this(new Random());
	}

	/** 테스트에서 랜덤 소스를 주입한다. */
	public ChoreDistributor(Random random) {
		this.random = random;
	}

	/**
	 * chores 를 members 에게 나눈다. 짐 하나씩 현재 총 개수(고정된 개수 포함)가 가장 적은 사람 중 한 명(동률이면 랜덤)에게 준다.
	 * 고정된 개수가 고르면 최종 개수 차이는 최대 1이다.
	 *
	 * @param fixedCounts 구성원별로 이미 고정된 짐 개수(없는 구성원은 0)
	 * @return 구성원 순서를 유지한 구성원별 새 배분. 짐이 없는 구성원은 빈 목록
	 */
	public <M, C> Map<M, List<C>> distribute(List<M> members, Map<M, Integer> fixedCounts, List<C> chores) {
		if (members.isEmpty()) {
			throw new IllegalArgumentException("배분할 구성원이 없습니다.");
		}
		Map<M, List<C>> result = new LinkedHashMap<>();
		Map<M, Integer> totals = new LinkedHashMap<>();
		for (M member : members) {
			result.put(member, new ArrayList<>());
			totals.put(member, fixedCounts.getOrDefault(member, 0));
		}

		List<C> shuffled = new ArrayList<>(chores);
		Collections.shuffle(shuffled, random);
		for (C chore : shuffled) {
			int min = Collections.min(totals.values());
			List<M> candidates = members.stream().filter(member -> totals.get(member) == min).toList();
			M picked = candidates.get(random.nextInt(candidates.size()));
			result.get(picked).add(chore);
			totals.merge(picked, 1, Integer::sum);
		}
		return result;
	}

}
