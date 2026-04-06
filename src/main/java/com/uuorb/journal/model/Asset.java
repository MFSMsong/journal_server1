package com.uuorb.journal.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Asset {
    Integer id;
    String assetId;
    String userId;
    Integer assetType;
    String name;
    String bankName;
    String cardLastFour;
    BigDecimal balance;
    String remark;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date updateTime;

    public static final int TYPE_CASH = 1;
    public static final int TYPE_SAVINGS_CARD = 2;
    public static final int TYPE_CREDIT_CARD = 3;
    public static final int TYPE_VIRTUAL = 4;
    public static final int TYPE_INVESTMENT = 5;
    public static final int TYPE_DEBT = 6;
    public static final int TYPE_RECEIVABLE = 7;
    public static final int TYPE_CUSTOM = 8;

    public boolean isLiability() {
        return assetType == TYPE_CREDIT_CARD || assetType == TYPE_DEBT;
    }
}
