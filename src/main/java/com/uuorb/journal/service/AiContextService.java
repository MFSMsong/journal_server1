package com.uuorb.journal.service;

import com.uuorb.journal.model.Activity;
import com.uuorb.journal.model.Asset;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiContextService {

    @Resource
    ActivityService activityService;

    @Resource
    AssetService assetService;

    /**
     * 构建财务数据上下文
     * 汇总用户的账本和资产信息，用于AI提供个性化理财建议
     *
     * @param userId 用户ID
     * @return 财务数据概览文本
     */
    public String buildFinancialContext(String userId) {
        StringBuilder sb = new StringBuilder();
        try {
            buildActivityContext(userId, sb);
            buildAssetContext(userId, sb);
        } catch (Exception e) {
            log.error("构建财务上下文失败: {}", e.getMessage());
        }
        return sb.toString();
    }

    /**
     * 构建账本上下文
     */
    private void buildActivityContext(String userId, StringBuilder sb) {
        List<Activity> myActivities = activityService.querySelfActivityList(Activity.builder().userId(userId).build());
        List<Activity> joinedActivities = activityService.queryJoinedActivityList(Activity.builder().userId(userId).build());

        if (myActivities.isEmpty() && joinedActivities.isEmpty()) {
            return;
        }

        sb.append("【我的账本】\n");
        for (Activity activity : myActivities) {
            double totalExpense = activity.getTotalExpense() != null ? activity.getTotalExpense().doubleValue() : 0;
            double totalIncome = activity.getTotalIncome() != null ? activity.getTotalIncome().doubleValue() : 0;
            sb.append(String.format("- %s: 总支出%.2f元, 总收入%.2f元",
                activity.getActivityName(), totalExpense, totalIncome));
            if (activity.getBudget() != null && activity.getBudget().doubleValue() > 0) {
                double budget = activity.getBudget().doubleValue();
                double remaining = activity.getRemainingBudget() != null ? activity.getRemainingBudget().doubleValue() : 0;
                sb.append(String.format(", 预算%.2f元, 剩余%.2f元", budget, remaining));
            }
            sb.append("\n");
        }

        if (!joinedActivities.isEmpty()) {
            sb.append("\n【加入的账本】\n");
            for (Activity activity : joinedActivities) {
                double totalExpense = activity.getTotalExpense() != null ? activity.getTotalExpense().doubleValue() : 0;
                double totalIncome = activity.getTotalIncome() != null ? activity.getTotalIncome().doubleValue() : 0;
                sb.append(String.format("- %s: 总支出%.2f元, 总收入%.2f元\n",
                    activity.getActivityName(), totalExpense, totalIncome));
            }
        }
    }

    /**
     * 构建资产上下文
     */
    private void buildAssetContext(String userId, StringBuilder sb) {
        List<Asset> assets = assetService.queryList(userId);
        if (assets.isEmpty()) {
            return;
        }

        sb.append("\n【我的资产】\n");

        double totalAsset = 0;
        double totalLiability = 0;

        for (Asset asset : assets) {
            double balance = asset.getBalance() != null ? asset.getBalance().doubleValue() : 0;
            String assetTypeName = getAssetTypeName(asset.getAssetType());
            String balanceStr = asset.isLiability()
                ? String.format("负债%.2f元", Math.abs(balance))
                : String.format("%.2f元", balance);
            sb.append(String.format("- %s(%s): %s\n", asset.getName(), assetTypeName, balanceStr));

            if (asset.isLiability()) {
                totalLiability += Math.abs(balance);
            } else {
                totalAsset += balance;
            }
        }

        double netAsset = totalAsset - totalLiability;
        sb.append(String.format("\n资产汇总: 总资产%.2f元, 总负债%.2f元, 净资产%.2f元\n",
            totalAsset, totalLiability, netAsset));
    }

    /**
     * 获取资产类型名称
     */
    private String getAssetTypeName(Integer assetType) {
        if (assetType == null) return "自定义";
        switch (assetType) {
            case Asset.TYPE_CASH: return "现金";
            case Asset.TYPE_SAVINGS_CARD: return "储蓄卡";
            case Asset.TYPE_CREDIT_CARD: return "信用卡";
            case Asset.TYPE_VIRTUAL: return "虚拟账户";
            case Asset.TYPE_INVESTMENT: return "投资账户";
            case Asset.TYPE_DEBT: return "负债";
            case Asset.TYPE_RECEIVABLE: return "债权";
            default: return "自定义";
        }
    }
}
