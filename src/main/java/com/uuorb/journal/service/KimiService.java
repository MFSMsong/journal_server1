package com.uuorb.journal.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONObject;
import com.uuorb.journal.model.AIConfig;
import com.uuorb.journal.model.EngelExpense;
import com.uuorb.journal.model.Expense;
import com.uuorb.journal.model.kimi.KimiMessage;
import com.uuorb.journal.util.KimiUtils;
import com.uuorb.journal.model.kimi.RoleEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kimi AI 服务实现类
 * 实现了 AiService 接口，提供基于 Kimi API 的各种AI功能
 * 包括：记账格式化、夸夸机器人、问候语、聊天对话等
 */
@Service("kimi")
public class KimiService implements AiService {

    @Resource
    KimiUtils kimiUtils;

    /** 记账格式化提示词：将自然语言转换为结构化的账单数据 */
    final static String FORMAT_PROMPT = "你是记账格式化机器人，请使用如下 JSON 格式输出你的回复： {type:string,price:double,label:string,positive:number,expenseTime:string}。positive0为支出，1为收入，expenseTime:yyyy-mm-dd hh:dd:ss。例：昨天从烟台打车到蓬莱176，返回{type:'交通',price:176.00,label:'打车从烟台到蓬莱',positive:0}，label保持原有意思不变，可以使得更容易理解，label尽量从列表中选择：美食,服装,捐赠,娱乐,燃料,房租,投资,宠物,化妆品,药品,电话费,购物,烟酒,学习,旅游,交通,其他，工资，红包，转账'}";

    /** 默认夸夸提示词：用于基础版本的夸夸功能 */
    final static String PRAISE_DEFAULT_PROMPT = "你是个夸夸机器人，你的角色是女儿，称呼我为爸爸，你的性格是活泼开朗。我会跟你说我的花销，你给我回应。字数在10-30左右";

    /** 可配置夸夸提示词模板：支持自定义角色、称呼和性格 */
    final static String PRAISE_PROMPT = "你是个夸夸机器人，你的角色是${relationship}，称呼我为${salutation}，你的性格是${personality}。我会跟你说我的花销，你给我回应。字数在10-30左右";

    /** 问候提示词模板：用于用户打开应用时的问候语 */
    final static String GREETING_PROMPT = "你的角色是${relationship}，称呼我为${salutation}，你的性格是${personality}。我们上次见面是${lastLoginTime}，不一定要强调上次见面时间，视情况而定，现在我走到你面前了，你给我打个招呼吧。字数在10-20左右";

    /** 恩格尔系数计算提示词 */
    final static String ENGEL_PROMPT = "帮我计算恩格尔系数;";

    /**
     * 将自然语言格式化为账单数据
     * 例如："昨天打车花了20块" -> Expense对象
     * 
     * @param naturalSentence 自然语言描述的消费记录
     * @return 解析后的Expense对象，解析失败返回null
     */
    @Override
    public Expense formatExpense(String naturalSentence) {
        // 获取当前时间，用于解析相对时间（如"昨天"）
        String now = LocalDateTimeUtil.formatNormal(LocalDateTime.now());
        
        // 构建消息列表
        List<KimiMessage> messages = CollUtil.newArrayList(
            new KimiMessage(RoleEnum.system.name(),
            FORMAT_PROMPT + "现在时间是: " + now),
            new KimiMessage(RoleEnum.user.name(), naturalSentence));

        // 调用Kimi API获取结构化数据
        KimiMessage chat = kimiUtils.chat(KIMI_MODEL, messages);
        try {
            return JSONObject.parseObject(chat.getContent(), Expense.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 基础版夸夸功能
     * 使用默认配置对用户的消费行为进行鼓励性回复
     * 
     * @param sentence 用户的消费描述
     * @return 夸夸回复内容
     */
    @Override
    public String praise(String sentence) {
        List<KimiMessage> messages = CollUtil.newArrayList(new KimiMessage(RoleEnum.system.name(),
            PRAISE_DEFAULT_PROMPT), new KimiMessage(RoleEnum.user.name(), sentence));
        KimiMessage chat = kimiUtils.chat(KIMI_MODEL, messages);
        return chat.getContent();
    }

    /**
     * 高级版夸夸功能
     * 根据用户配置的角色、称呼、性格生成个性化回复
     * 
     * @param sentence 用户的消费描述
     * @param config 用户AI配置（角色、称呼、性格）
     * @return 个性化夸夸回复
     */
    public String praise(String sentence, AIConfig config) {
        // 替换提示词中的占位符
        var prompt = PRAISE_PROMPT.replace("${relationship}", config.getRelationship())
            .replace("${personality}", config.getPersonality())
            .replace("${salutation}", config.getSalutation());
        List<KimiMessage> messages = CollUtil.newArrayList(new KimiMessage(RoleEnum.system.name(), prompt),
            new KimiMessage(RoleEnum.user.name(), sentence));
        KimiMessage chat = kimiUtils.chat(KIMI_MODEL, messages);
        return chat.getContent();
    }

    /**
     * 生成问候语
     * 根据用户配置和上次登录时间生成个性化问候
     * 
     * @param config 用户AI配置
     * @return 问候语内容
     */
    public String greet(AIConfig config) {
        String prompt = GREETING_PROMPT.replace("${relationship}", config.getRelationship())
            .replace("${personality}", config.getPersonality())
            .replace("${salutation}", config.getSalutation())
            .replace("${lastLoginTime}", config.getLastLoginTime());

        List<KimiMessage> messages = CollUtil.newArrayList(new KimiMessage(RoleEnum.system.name(), prompt));
        KimiMessage chat = kimiUtils.chat(KIMI_MODEL, messages);
        return chat.getContent();
    }

    @Override
    public String generateImage(String model, String description, String role) {
        return "not implement";
    }

    /**
     * 流式夸夸功能
     * 通过SSE方式实时返回夸夸内容，实现打字机效果
     * 
     * @param sentence 用户的消费描述
     * @param config 用户AI配置
     * @param outputStream 输出流
     */
    @Override
    public void praiseStream(String sentence, AIConfig config, OutputStream outputStream) {
        var prompt = PRAISE_PROMPT.replace("${relationship}", config.getRelationship())
            .replace("${personality}", config.getPersonality())
            .replace("${salutation}", config.getSalutation());
        List<KimiMessage> messages = CollUtil.newArrayList(new KimiMessage(RoleEnum.system.name(), prompt),
            new KimiMessage(RoleEnum.user.name(), sentence));
        kimiUtils.chatInStream(KIMI_MODEL, messages, outputStream);
    }

    @Override
    public String tts(String sentence, AIConfig aiConfig) {
        return "";
    }

    /**
     * 恩格尔系数解释
     * 根据用户的消费数据计算并解释恩格尔系数
     * 
     * @param expenses 消费数据列表
     * @param outputStream 输出流
     */
    @Override
    public void engelExplain(List<EngelExpense> expenses, OutputStream outputStream) {
        List<KimiMessage> messages = CollUtil.newArrayList(new KimiMessage(RoleEnum.system.name(), ENGEL_PROMPT),
            new KimiMessage(RoleEnum.user.name(), expenses.toString()));
        kimiUtils.chatInStream(KIMI_MODEL, messages, outputStream);
    }

    /** AI聊天提示词模板：定义AI助手的角色和行为 */
    final static String CHAT_PROMPT = "你是一个智能理财助手，你的角色是${relationship}，称呼我为${salutation}，你的性格是${personality}。" +
        "你可以根据用户的财务数据提供理财建议，也可以和用户进行日常聊天。" +
        "回答要简洁友好，字数控制在100字以内。" +
        "如果用户询问理财相关问题，请结合提供的财务数据给出建议。" +
        "如果用户只是闲聊，就正常回复即可。";

    /**
     * AI聊天功能（流式响应）
     * 支持结合用户财务数据进行个性化对话
     * 
     * @param message 用户消息
     * @param config 用户AI配置（角色、称呼、性格）
     * @param financialContext 财务数据上下文（可选）
     * @param outputStream 输出流，用于写入响应内容
     */
    @Override
    public void chatStream(String message, AIConfig config, String financialContext, OutputStream outputStream) {
        // 构建系统提示词，设置AI角色
        String systemPrompt = CHAT_PROMPT
            .replace("${relationship}", config.getRelationship() != null ? config.getRelationship() : "朋友")
            .replace("${personality}", config.getPersonality() != null ? config.getPersonality() : "热情友好")
            .replace("${salutation}", config.getSalutation() != null ? config.getSalutation() : "朋友");

        List<KimiMessage> messages = CollUtil.newArrayList(
            new KimiMessage(RoleEnum.system.name(), systemPrompt)
        );

        // 如果提供了财务数据，添加到上下文中
        if (financialContext != null && !financialContext.isEmpty()) {
            messages.add(new KimiMessage(RoleEnum.system.name(), "以下是用户的财务数据概览，供参考：\n" + financialContext));
        }

        // 添加用户消息
        messages.add(new KimiMessage(RoleEnum.user.name(), message));

        // 调用Kimi流式API
        kimiUtils.chatInStream(KIMI_MODEL, messages, outputStream);
    }
}
