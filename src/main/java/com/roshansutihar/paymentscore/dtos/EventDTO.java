package com.roshansutihar.paymentscore.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventDTO {
    private String eventType;
    private LocalDateTime timestamp;
    private String payload;
}
