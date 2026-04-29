package com.uuorb.journal.websocket;

import cn.hutool.json.JSONUtil;
import com.uuorb.journal.model.ws.WebSocketMessage;
import com.uuorb.journal.util.TokenUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ServerEndpoint("/ws/journal")
public class JournalWebSocketEndpoint {

    private static final Map<String, Set<Session>> activitySessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> userActivities = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("WebSocket 连接建立: sessionId={}, uri={}", session.getId(), session.getRequestURI());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到 WebSocket 消息: sessionId={}, message={}", session.getId(), message);
        
        try {
            Map<String, Object> data = JSONUtil.toBean(message, Map.class);
            String action = (String) data.get("action");
            
            log.info("处理 WebSocket 动作: sessionId={}, action={}", session.getId(), action);
            
            switch (action) {
                case "auth" -> handleAuth(session, (String) data.get("token"));
                case "subscribe" -> handleSubscribe(session, (String) data.get("activityId"));
                case "unsubscribe" -> handleUnsubscribe(session, (String) data.get("activityId"));
                default -> log.warn("未知操作: {}", action);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        String userId = sessionToUser.remove(session.getId());
        log.info("WebSocket 连接关闭: sessionId={}, userId={}", session.getId(), userId);
        
        if (userId != null) {
            Set<String> activities = userActivities.remove(userId);
            if (activities != null) {
                for (String activityId : activities) {
                    Set<Session> sessions = activitySessions.get(activityId);
                    if (sessions != null) {
                        sessions.remove(session);
                        if (sessions.isEmpty()) {
                            activitySessions.remove(activityId);
                        }
                    }
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误: sessionId={}, error={}", session.getId(), error.getMessage(), error);
    }

    private void handleAuth(Session session, String token) {
        try {
            log.info("处理认证请求: sessionId={}", session.getId());
            
            String userId = TokenUtil.getUserId(token);
            boolean isExpired = TokenUtil.isTokenExpired(token);
            
            log.info("Token解析结果: userId={}, isExpired={}", userId, isExpired);
            
            if (userId != null && !isExpired) {
                sessionToUser.put(session.getId(), userId);
                userActivities.putIfAbsent(userId, ConcurrentHashMap.newKeySet());
                
                String responseJson = JSONUtil.toJsonStr(WebSocketMessage.<String>builder()
                        .type("AUTH_SUCCESS")
                        .data(userId)
                        .timestamp(System.currentTimeMillis())
                        .build());
                
                log.info("发送认证成功响应: sessionId={}, userId={}", session.getId(), userId);
                session.getBasicRemote().sendText(responseJson);
            } else {
                String responseJson = JSONUtil.toJsonStr(WebSocketMessage.<String>builder()
                        .type("AUTH_FAILED")
                        .data("Token无效")
                        .timestamp(System.currentTimeMillis())
                        .build());
                
                log.warn("认证失败: sessionId={}", session.getId());
                session.getBasicRemote().sendText(responseJson);
                session.close();
            }
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage(), e);
        }
    }

    private void handleSubscribe(Session session, String activityId) {
        String userId = sessionToUser.get(session.getId());
        log.info("处理订阅请求: sessionId={}, userId={}, activityId={}", session.getId(), userId, activityId);
        
        if (userId == null) {
            log.warn("未认证用户尝试订阅: sessionId={}", session.getId());
            return;
        }
        
        Set<Session> sessions = activitySessions.computeIfAbsent(activityId, k -> ConcurrentHashMap.newKeySet());
        sessions.add(session);
        userActivities.get(userId).add(activityId);
        
        log.info("订阅成功: userId={}, activityId={}, 当前账本在线人数={}", userId, activityId, sessions.size());
        
        try {
            String responseJson = JSONUtil.toJsonStr(WebSocketMessage.<String>builder()
                    .type("SUBSCRIBE_SUCCESS")
                    .activityId(activityId)
                    .data(activityId)
                    .timestamp(System.currentTimeMillis())
                    .build());
            session.getBasicRemote().sendText(responseJson);
        } catch (IOException e) {
            log.error("发送订阅成功响应失败: {}", e.getMessage());
        }
    }

    private void handleUnsubscribe(Session session, String activityId) {
        String userId = sessionToUser.get(session.getId());
        if (userId == null) {
            return;
        }
        
        Set<Session> sessions = activitySessions.get(activityId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                activitySessions.remove(activityId);
            }
        }
        
        Set<String> activities = userActivities.get(userId);
        if (activities != null) {
            activities.remove(activityId);
        }
        
        log.info("用户取消订阅账本: userId={}, activityId={}", userId, activityId);
    }

    public static void sendToActivity(String activityId, WebSocketMessage<?> message) {
        Set<Session> sessions = activitySessions.get(activityId);
        log.info("推送消息到账本: activityId={}, 在线人数={}", activityId, sessions != null ? sessions.size() : 0);
        
        if (sessions == null || sessions.isEmpty()) {
            log.warn("账本没有在线用户: activityId={}", activityId);
            return;
        }
        
        String json = JSONUtil.toJsonStr(message);
        
        int successCount = 0;
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                    successCount++;
                } catch (IOException e) {
                    log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
        
        log.info("推送完成: activityId={}, 成功数={}/{}", activityId, successCount, sessions.size());
    }

    public static void sendToActivityExcept(String activityId, WebSocketMessage<?> message, String excludeUserId) {
        Set<Session> sessions = activitySessions.get(activityId);
        log.info("推送消息到账本(排除操作者): activityId={}, 在线人数={}, excludeUserId={}", 
                activityId, sessions != null ? sessions.size() : 0, excludeUserId);
        
        if (sessions == null || sessions.isEmpty()) {
            log.warn("账本没有在线用户: activityId={}", activityId);
            return;
        }
        
        String json = JSONUtil.toJsonStr(message);
        
        int successCount = 0;
        int skippedCount = 0;
        for (Session session : sessions) {
            String userId = sessionToUser.get(session.getId());
            if (excludeUserId != null && excludeUserId.equals(userId)) {
                skippedCount++;
                continue;
            }
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                    successCount++;
                } catch (IOException e) {
                    log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
        
        log.info("推送完成: activityId={}, 成功数={}, 跳过操作者数={}", activityId, successCount, skippedCount);
    }

    public static void sendToUser(String userId, WebSocketMessage<?> message) {
        Set<String> activities = userActivities.get(userId);
        if (activities == null || activities.isEmpty()) {
            return;
        }
        
        String json = JSONUtil.toJsonStr(message);
        
        for (String activityId : activities) {
            Set<Session> sessions = activitySessions.get(activityId);
            if (sessions != null) {
                for (Session session : sessions) {
                    String sessionUserId = sessionToUser.get(session.getId());
                    if (userId.equals(sessionUserId) && session.isOpen()) {
                        try {
                            session.getBasicRemote().sendText(json);
                        } catch (IOException e) {
                            log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
