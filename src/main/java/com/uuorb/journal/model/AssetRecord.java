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
public class AssetRecord {
    Integer id;
    String recordId;
    String assetId;
    String userId;
    Integer operationType;
    BigDecimal beforeBalance;
    BigDecimal afterBalance;
    BigDecimal changeAmount;
    String remark;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date createTime;

    public static final int OP_MANUAL_ADJUST = 1;
    public static final int OP_EXPENSE = 2;
    public static final int OP_INCOME = 3;
    public static final int OP_TRANSFER_IN = 4;
    public static final int OP_TRANSFER_OUT = 5;
}
