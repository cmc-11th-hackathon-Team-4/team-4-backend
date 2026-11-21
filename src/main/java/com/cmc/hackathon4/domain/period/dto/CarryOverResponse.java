package com.cmc.hackathon4.domain.period.dto;

import java.util.List;

public record CarryOverResponse(List<CarryOverRequest.Choice> choices) {
}
