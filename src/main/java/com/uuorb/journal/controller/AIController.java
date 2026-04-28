package com.uuorb.journal.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.thread.ThreadUtil;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.Log;
import com.uuorb.journal.annotation.UserId;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.mapper.ExpenseMapper;
import com.uuorb.journal.model.AIConfig;
import com.uuorb.journal.model.Activity;
import com.uuorb.journal.model.EngelExpense;
import com.uuorb.journal.model.Expense;
import com.uuorb.journal.model.User;
import com.uuorb.journal.service.ActivityService;
import com.uuorb.journal.service.AiService;
import com.uuorb.journal.service.ExpenseService;
import com.uuorb.journal.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

import static com.uuorb.journal.constant.ResultStatus.AI_FORMAT_ERROR;

/**
 * AI功能控制器
 * 提供AI相关的各种接口：记账格式化、夸夸、聊天、语音合成、图片生成等
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

    /** Kimi AI服务（月之暗面） */
    @Resource(name = "kimi")
    AiService kimiService;

    /** Coze AI服务（字节跳动） */
    @Resource(name = "coze")
    AiService cozeService;

    @Resource
    ActivityService activityService;

    @Resource
    ExpenseService expenseService;

    @Resource
    UserService userService;

    @Resource
    ExpenseMapper expenseMapper;

    /**
     * AI记账格式化接口
     * 将自然语言描述的消费记录转换为结构化账单数据
     * 例如："昨天打车花了20块" -> {type: "交通", price: 20, ...}
     * 
     * @param sentence 自然语言描述
     * @param activityId 账本ID
     * @param userId 用户ID
     * @return 结构化后的账单数据
     */
    @Log
    @Authorization
    @GetMapping("/format")
    Result<Expense> format(@RequestParam String sentence, @RequestParam String activityId, @UserId String userId) {
        log.info("==> 结构化:{} ", sentence);

        Expense expense = kimiService.formatExpense(sentence);

        if (expense == null) {
            log.error("结构化失败：{}", sentence);
            return Result.error(AI_FORMAT_ERROR);
        }

        // 填充账单所需的关联信息
        expense.setActivityId(activityId);
        expense.setUserId(userId);
        expense.setCreateTime(new DateTime());
        expense.setUpdateTime(new DateTime());

        // 异步插入数据库并更新预算
        ThreadUtil.execAsync(() -> {
            expenseService.insertExpenseAndCalcRemainingBudget(expense);
        });

        return Result.ok(expense);
    }

    /**
     * 基础版夸夸接口
     * 使用默认配置对用户的消费行为进行鼓励性回复
     * 
     * @param sentence 用户的消费描述
     * @return 夸夸回复内容
     */
    @Log
    @GetMapping("/praise")
    Result<String> praise(@RequestParam String sentence) {
        String response = kimiService.praise(sentence);
        return Result.ok(response);
    }

    /**
     * 高级版夸夸接口
     * 根据用户配置的角色、称呼、性格生成个性化回复
     * 
     * @param sentence 用户的消费描述
     * @param activityId 账本ID（用于获取用户上下文）
     * @param userId 用户ID
     * @return 个性化夸夸回复
     */
    @Log
    @Authorization
    @GetMapping("/praise/advance")
    Result<String> praiseAdvance(@RequestParam String sentence, @RequestParam String activityId,
        @UserId String userId) {
        // 获取用户配置
        User userProfile = userService.getUserByUserId(userId);
        AIConfig aiConfig = AIConfig.of(userProfile);
        String response = kimiService.praise(sentence, aiConfig);
        return Result.ok(response);
    }

    /**
     * 流式夸夸接口
     * 通过SSE方式实时返回夸夸内容，实现打字机效果
     * 
     * @param sentence 用户的消费描述
     * @param userId 用户ID
     * @return SSE流式响应
     */
    @Log
    @Authorization
    @GetMapping("/praise/stream")
    ResponseEntity<StreamingResponseBody> praiseStream(@RequestParam String sentence, @UserId String userId) {
        User userProfile = userService.getUserByUserId(userId);
        AIConfig aiConfig = AIConfig.of(userProfile);
        
        // 返回SSE流式响应
        return ResponseEntity.ok()
            .header("content-type", MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
            .body(outputStream -> {
                try {
                    kimiService.praiseStream(sentence, aiConfig, outputStream);
                } catch (Exception e) {
                    log.error("夸夸失败:{},{}", sentence, e.getMessage());
                }
            });
    }

    /**
     * 恩格尔系数计算接口
     * 根据账本消费数据计算并解释恩格尔系数
     * 
     * @param activityId 账本ID
     * @return SSE流式响应，包含恩格尔系数解释
     */
    @Log
    @GetMapping("/engel/{activityId}")
    ResponseEntity<StreamingResponseBody> engel(@PathVariable String activityId) {
        List<EngelExpense> expenseList = expenseMapper.queryListBrief(activityId);
        return ResponseEntity.ok()
            .header("content-type", MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
            .body(outputStream -> {
                try {
                    kimiService.engelExplain(expenseList, outputStream);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });
    }

    /**
     * 语音合成接口（TTS）
     * 将文本转换为语音
     * 
     * @param sentence 要转换的文本
     * @param activityId 账本ID
     * @param userId 用户ID
     * @return 语音数据（Base64编码）
     */
    @Log
    @GetMapping("/tts")
    Result<String> tts(@RequestParam String sentence, @RequestParam String activityId, @UserId String userId) {
        User userProfile = userService.getUserByUserId(userId);
        AIConfig aiConfig = AIConfig.of(userProfile);

        return Result.ok(cozeService.tts(sentence, aiConfig));
    }

    /**
     * AI图片生成接口
     * 根据描述生成图片
     * 
     * @param model 模型名称
     * @param description 图片描述
     * @param role 角色设定
     * @return 生成的图片URL或错误信息
     */
    @Log
    @Authorization
    @GetMapping("/image")
    Result<String> generateImage(@RequestParam String model, @RequestParam String description,
        @RequestParam String role) {
        String result = cozeService.generateImage(model, description, role);
        if (result.startsWith("http")) {
            return Result.ok(result);
        } else {
            return Result.error(805, result);
        }
    }

    /**
     * AI问候语接口
     * 用户打开应用时生成个性化问候语
     * 
     * @param userId 用户ID
     * @return 问候语内容
     */
    @Log
    @Authorization
    @GetMapping("/greeting")
    Result<String> praiseAdvance(@UserId String userId) {
        User userProfile = userService.getUserByUserId(userId);
        AIConfig aiConfig = AIConfig.of(userProfile);
        String response = kimiService.greet(aiConfig);
        return Result.ok(response);
    }

    /**
     * 本地分词接口
     * 使用结巴分词对文本进行分词处理
     * 
     * @param sentence 要分词的文本
     * @return 分词结果列表
     */
    @GetMapping("/format/local")
    Result<List<SegToken>> formatLocal(@RequestParam("sentence") String sentence) {
        List<SegToken> process = new JiebaSegmenter().process(sentence, JiebaSegmenter.SegMode.INDEX);
        return Result.ok(process);
    }

    /**
     * AI聊天接口（流式响应）
     * 支持结合用户财务数据进行个性化对话
     * 通过SSE方式实时返回AI回复，实现打字机效果
     * 
     * @param message 用户消息
     * @param includeFinancialData 是否包含财务数据上下文
     * @param userId 用户ID
     * @return SSE流式响应
     */
    @Log
    @Authorization
    @GetMapping("/chat")
    ResponseEntity<StreamingResponseBody> chat(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "false") boolean includeFinancialData,
            @UserId String userId) {
        // 获取用户配置（角色、称呼、性格）
        User userProfile = userService.getUserByUserId(userId);
        AIConfig aiConfig = AIConfig.of(userProfile);

        // 可选：构建财务数据上下文
        String financialContext = "";
        if (includeFinancialData) {
            financialContext = buildFinancialContext(userId);
        }

        final String context = financialContext;
        
        // 返回SSE流式响应
        return ResponseEntity.ok()
            .header("content-type", MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
            .body(outputStream -> {
                try {
                    kimiService.chatStream(message, aiConfig, context, outputStream);
                } catch (Exception e) {
                    log.error("AI聊天失败:{},{}", message, e.getMessage());
                }
            });
    }

    /**
     * 构建财务数据上下文
     * 汇总用户的账本信息，用于AI提供个性化理财建议
     * 
     * @param userId 用户ID
     * @return 财务数据概览文本
     */
    private String buildFinancialContext(String userId) {
        StringBuilder sb = new StringBuilder();
        try {
            // 获取用户创建的账本和加入的账本
            List<Activity> myActivities = activityService.querySelfActivityList(Activity.builder().userId(userId).build());
            List<Activity> joinedActivities = activityService.queryJoinedActivityList(Activity.builder().userId(userId).build());

            if (!myActivities.isEmpty() || !joinedActivities.isEmpty()) {
                // 我创建的账本
                sb.append("【我的账本】\n");
                for (Activity activity : myActivities) {
                    double totalExpense = activity.getTotalExpense() != null ? activity.getTotalExpense().doubleValue() : 0;
                    double totalIncome = activity.getTotalIncome() != null ? activity.getTotalIncome().doubleValue() : 0;
                    sb.append(String.format("- %s: 总支出%.2f元, 总收入%.2f元",
                        activity.getActivityName(),
                        totalExpense,
                        totalIncome));
                    // 如果有预算，显示预算和剩余
                    if (activity.getBudget() != null && activity.getBudget().doubleValue() > 0) {
                        double budget = activity.getBudget().doubleValue();
                        double remaining = activity.getRemainingBudget() != null ? activity.getRemainingBudget().doubleValue() : 0;
                        sb.append(String.format(", 预算%.2f元, 剩余%.2f元", budget, remaining));
                    }
                    sb.append("\n");
                }

                // 我加入的账本
                if (!joinedActivities.isEmpty()) {
                    sb.append("\n【加入的账本】\n");
                    for (Activity activity : joinedActivities) {
                        double totalExpense = activity.getTotalExpense() != null ? activity.getTotalExpense().doubleValue() : 0;
                        double totalIncome = activity.getTotalIncome() != null ? activity.getTotalIncome().doubleValue() : 0;
                        sb.append(String.format("- %s: 总支出%.2f元, 总收入%.2f元\n",
                            activity.getActivityName(),
                            totalExpense,
                            totalIncome));
                    }
                }
            }
        } catch (Exception e) {
            log.error("构建财务上下文失败: {}", e.getMessage());
        }
        return sb.toString();
    }
}
