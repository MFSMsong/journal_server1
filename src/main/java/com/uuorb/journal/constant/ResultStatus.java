package com.uuorb.journal.constant;

import lombok.Getter;

@Getter
public enum ResultStatus {
    SUCCESS_STATUS(0, "SUCCESS"),

    //////////////////// 客户端错误 /////////////////////////
    TOKEN_VALID(-1, "TOKEN失效"),
    PARAM_VALID(401, "参数错误"),
    PARAM_MISS(402, "参数缺失或格式错误，多见于末尾有多余逗号"),
    PARAM_TOO_LONG(403, "参数太多"),
    RESOURCE_NOT_FOUND(404, "资源未找到"),
    AI_FORMAT_ERROR(405, "信息提取失败"),

    NOT_OWN_RESOURCE(405, "无资源访问权限，或不存在"),
    DONT_MODIFY_OFFICIAL_BOOK(406, "不能修改官方词书"),

    ACTION_REPEAT(407, "重复点赞或取消"),
    DELETE_DEFAULT_GROUP(408, "不能删除默认分组"),

    VIP_PERMISSION(409, "VIP权限不足"),
    OVER_MAX_COUNT(410, "容量已达上限，请解锁会员后重试"),
    NOT_SUBSCRIBE(411, "请先关注公众号"),
    BOOK_END(412, "词书已背完"),
    FILE_TOO_LARGE(413, "文件过大"),
    VERIFY_CODE_LIMITED(414, "验证码发送过于频繁，请稍后再试"),
    VERIFY_CODE_ERROR(415, "验证码错误"),
    TELEPHONE_ERROR(416, "手机号不符合规范"),
    EMAIL_ERROR(417, "邮箱不符合规范"),
    ACCOUNT_ERROR(418, "账号格式错误"),
    PAYMENT_INDEX_NULL(419, "未选择支付方式"),
    PAYMENT_INDEX_INVALID(420, "支付参数错误"),
    PAYMENT_NOT_PAID(421, "支付未完成"),
    OPERATION_ERROR(422, "微信支付操作失败"),
    ORDER_NO_NOT_EXIST(423, "订单不存在"),
    IOS_SECRET_INVALID(424, "苹果支付验证失败"),
    PUNCH_ALREADY_PUNCHED(425, "今日已打卡"),
    PUNCH_ALREADY_BOUGHT(426, "不要重复购买"),
    NOT_ENOUGH_POINT(427, "积分不足"),

    ///////////////////// 服务端错误 ////////////////////////
    GENERAL_ERROR(500, "系统错误"),

    GET_OUT(-999, "非法攻击"),

    IOS_JWT_INVALID(428, "苹果登录失败"),
    PRIMARY_ID_MISS(429, "未传递主键"),
    FORBID_JOIN_SELF_ACTIVITY(430, "不允许加入自己的活动"),
    JOIN_REPEAT(431, "已经加入该活动"), 
    OWNER_CANT_EXIT(432, "创建者只能删除账本");

    ResultStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private Integer code;

    private String msg;

}
