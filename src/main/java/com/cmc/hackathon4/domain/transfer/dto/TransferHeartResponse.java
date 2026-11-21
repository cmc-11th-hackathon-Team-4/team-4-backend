package com.cmc.hackathon4.domain.transfer.dto;

import java.time.Instant;

/** 하트 전송 결과 */
public record TransferHeartResponse(Long transferId, Instant heartedAt) {
}
