package com.cmc.hackathon4.domain.period.service;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 0시(KST)에 결산일이 지난 진행 중 기간을 자동으로 종료한다. 기간 종료 버튼(#28)과 같은 로직을 쓴다.
 * 날짜(startDate, endDate)가 KST 기준이라 "오늘"도 KST 로 계산한다. 서버 시계와 저장 시각은 UTC 그대로다.
 * 서버가 0시에 꺼져 있었더라도 다음 0시에 지난 기간을 모두 종료한다.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PeriodAutoEndScheduler {

	private static final String KST = "Asia/Seoul";

	private final PeriodService periodService;

	@Scheduled(cron = "0 0 0 * * *", zone = KST)
	public void endExpiredPeriods() {
		endExpiredPeriods(LocalDate.now(ZoneId.of(KST)));
	}

	/** today 보다 결산일이 앞선 기간을 하나씩 따로 종료한다. 한 집에서 실패해도 나머지는 계속한다. */
	public int endExpiredPeriods(LocalDate today) {
		int ended = 0;
		for (Long periodId : periodService.findExpiredPeriodIds(today)) {
			try {
				if (periodService.endIfExpired(periodId, today)) {
					ended++;
				}
			}
			catch (RuntimeException e) {
				log.error("[PeriodAutoEnd] 기간 자동 종료 실패: periodId={}", periodId, e);
			}
		}
		log.info("[PeriodAutoEnd] 결산일이 지난 기간 {}개를 종료했습니다.", ended);
		return ended;
	}

}
