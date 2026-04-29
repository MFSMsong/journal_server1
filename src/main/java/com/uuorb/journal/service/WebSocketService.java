package com.uuorb.journal.service;

import com.uuorb.journal.model.Activity;
import com.uuorb.journal.model.ActivityMember;
import com.uuorb.journal.model.Expense;
import com.uuorb.journal.model.User;
import com.uuorb.journal.model.ws.MessageType;
import com.uuorb.journal.model.ws.WebSocketMessage;
import com.uuorb.journal.websocket.JournalWebSocketEndpoint;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WebSocketService {

    public void notifyExpenseAdd(String activityId, Expense expense, String excludeUserId) {
        log.info("推送账单添加通知: activityId={}, expenseId={}", activityId, expense.getExpenseId());
        JournalWebSocketEndpoint.sendToActivityExcept(
                activityId,
                WebSocketMessage.of(MessageType.EXPENSE_ADD, activityId, expense),
                excludeUserId
        );
    }

    public void notifyExpenseUpdate(String activityId, Expense expense, String excludeUserId) {
        log.info("推送账单更新通知: activityId={}, expenseId={}", activityId, expense.getExpenseId());
        JournalWebSocketEndpoint.sendToActivityExcept(
                activityId,
                WebSocketMessage.of(MessageType.EXPENSE_UPDATE, activityId, expense),
                excludeUserId
        );
    }

    public void notifyExpenseDelete(String activityId, String expenseId, String excludeUserId) {
        log.info("推送账单删除通知: activityId={}, expenseId={}", activityId, expenseId);
        Map<String, String> data = new HashMap<>();
        data.put("expenseId", expenseId);
        JournalWebSocketEndpoint.sendToActivityExcept(
                activityId,
                WebSocketMessage.of(MessageType.EXPENSE_DELETE, activityId, data),
                excludeUserId
        );
    }

    public void notifyActivityUpdate(String activityId, Activity activity) {
        log.info("推送账本更新通知: activityId={}", activityId);
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.ACTIVITY_UPDATE, activityId, activity)
        );
    }

    public void notifyActivityDelete(String activityId) {
        log.info("推送账本删除通知: activityId={}", activityId);
        Map<String, String> data = new HashMap<>();
        data.put("activityId", activityId);
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.ACTIVITY_DELETE, activityId, data)
        );
    }

    public void notifyMemberJoin(String activityId, ActivityMember member) {
        log.info("推送成员加入通知: activityId={}, userId={}", activityId, member.getUserId());
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.MEMBER_JOIN, activityId, member)
        );
    }

    public void notifyMemberExit(String activityId, String userId, String nickname) {
        log.info("推送成员退出通知: activityId={}, userId={}", activityId, userId);
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("nickname", nickname);
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.MEMBER_EXIT, activityId, data)
        );
    }

    public void notifyMemberKick(String activityId, String userId, String nickname) {
        log.info("推送成员被踢通知: activityId={}, userId={}", activityId, userId);
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("nickname", nickname);
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.MEMBER_KICK, activityId, data)
        );
    }

    public void notifyMemberNicknameUpdate(String activityId, String userId, String nickname) {
        log.info("推送成员昵称更新通知: activityId={}, userId={}", activityId, userId);
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("nickname", nickname);
        JournalWebSocketEndpoint.sendToActivity(
                activityId,
                WebSocketMessage.of(MessageType.MEMBER_NICKNAME_UPDATE, activityId, data)
        );
    }

    public void notifyUserUpdate(String userId, User user) {
        log.info("推送用户信息更新通知: userId={}", userId);
        JournalWebSocketEndpoint.sendToUser(
                userId,
                WebSocketMessage.of(MessageType.USER_UPDATE, null, user)
        );
    }
}
