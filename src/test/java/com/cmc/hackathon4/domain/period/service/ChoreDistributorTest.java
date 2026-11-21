package com.cmc.hackathon4.domain.period.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import com.cmc.hackathon4.domain.house.entity.House;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChoreDistributorTest {

	@Test
	void 고정된_짐이_없으면_개수_차이가_최대_1이다() {
		for (int seed = 0; seed < 50; seed++) {
			ChoreDistributor distributor = new ChoreDistributor(new Random(seed));
			for (int members = 1; members <= House.MAX_MEMBERS; members++) {
				for (int chores = 0; chores <= 13; chores++) {
					Map<Integer, List<Integer>> result = distributor.distribute(range(members), new HashMap<>(),
							range(chores));

					assertThat(result.values().stream().mapToInt(List::size).sum()).isEqualTo(chores);
					assertThat(sizeGap(result, new HashMap<>())).isLessThanOrEqualTo(1);
				}
			}
		}
	}

	@Test
	void 모든_집안일이_정확히_한_번씩만_배정된다() {
		ChoreDistributor distributor = new ChoreDistributor(new Random(1));

		Map<Integer, List<Integer>> result = distributor.distribute(range(3), new HashMap<>(), range(10));

		assertThat(result.values().stream().flatMap(List::stream)).containsExactlyInAnyOrderElementsOf(range(10));
	}

	@Test
	void 고정된_짐을_포함한_총_개수가_균등해지도록_나머지를_나눈다() {
		Map<Integer, Integer> fixed = Map.of(0, 3, 1, 1, 2, 0);
		for (int seed = 0; seed < 50; seed++) {
			ChoreDistributor distributor = new ChoreDistributor(new Random(seed));

			// 고정 4 + 나머지 5 = 9, 1인당 3개
			Map<Integer, List<Integer>> result = distributor.distribute(range(3), fixed, range(5));

			assertThat(result.get(0)).isEmpty();
			assertThat(result.get(1)).hasSize(2);
			assertThat(result.get(2)).hasSize(3);
			assertThat(sizeGap(result, fixed)).isZero();
		}
	}

	@Test
	void 고정된_짐이_고르지_않아도_총_개수_차이는_최소로_줄어든다() {
		Map<Integer, Integer> fixed = Map.of(0, 2, 1, 0);
		ChoreDistributor distributor = new ChoreDistributor(new Random(7));

		// 고정 2 + 나머지 3 = 5, 3/2 로 나뉘어야 한다
		Map<Integer, List<Integer>> result = distributor.distribute(range(2), fixed, range(3));

		assertThat(sizeGap(result, fixed)).isLessThanOrEqualTo(1);
	}

	@Test
	void 같은_시드면_같은_결과가_나온다() {
		Map<Integer, List<Integer>> first = new ChoreDistributor(new Random(42)).distribute(range(3), new HashMap<>(),
				range(8));
		Map<Integer, List<Integer>> second = new ChoreDistributor(new Random(42)).distribute(range(3),
				new HashMap<>(), range(8));

		assertThat(first).isEqualTo(second);
	}

	private static List<Integer> range(int count) {
		return IntStream.range(0, count).boxed().toList();
	}

	/** 고정 개수까지 합친 구성원별 총 개수의 최대-최소 */
	private static int sizeGap(Map<Integer, List<Integer>> result, Map<Integer, Integer> fixed) {
		List<Integer> totals = result.entrySet()
				.stream()
				.map(entry -> entry.getValue().size() + fixed.getOrDefault(entry.getKey(), 0))
				.toList();
		return totals.stream().max(Integer::compare).orElse(0) - totals.stream().min(Integer::compare).orElse(0);
	}

}
