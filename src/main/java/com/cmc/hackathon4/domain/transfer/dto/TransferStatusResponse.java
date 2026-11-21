package com.cmc.hackathon4.domain.transfer.dto;

import com.cmc.hackathon4.domain.transfer.entity.TransferStatus;

/** 부탁 생성·거절 결과: transfer ID 와 상태 */
public record TransferStatusResponse(Long transferId, TransferStatus status) {
}
