package com.uuorb.journal.config;

import com.uuorb.journal.websocket.JournalWebSocketEndpoint;
import jakarta.websocket.server.ServerContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import jakarta.websocket.server.ServerEndpointConfig;

@Slf4j
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        log.info("=== 创建 ServerEndpointExporter Bean ===");
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        
        exporter.setAnnotatedEndpointClasses(JournalWebSocketEndpoint.class);
        
        log.info("=== ServerEndpointExporter 创建成功，已注册端点类: {} ===", JournalWebSocketEndpoint.class.getName());
        return exporter;
    }
}
