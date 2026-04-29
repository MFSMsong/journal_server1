package com.uuorb.journal.model.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    
    private String type;
    
    private String activityId;
    
    private T data;
    
    private Long timestamp;
    
    public static <T> WebSocketMessage<T> of(String type, String activityId, T data) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .activityId(activityId)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
