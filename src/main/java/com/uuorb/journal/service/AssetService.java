package com.uuorb.journal.service;

import com.uuorb.journal.mapper.AssetMapper;
import com.uuorb.journal.model.Asset;
import com.uuorb.journal.model.AssetRecord;
import com.uuorb.journal.util.IDUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetService {

    @Resource
    AssetMapper assetMapper;

    public List<Asset> queryList(String userId) {
        return assetMapper.queryList(userId);
    }

    public Asset queryById(String assetId) {
        return assetMapper.queryById(assetId);
    }

    @Transactional
    public Asset create(Asset asset) {
        asset.setAssetId(IDUtil.assetId());
        assetMapper.insert(asset);
        return assetMapper.queryById(asset.getAssetId());
    }

    @Transactional
    public void update(Asset asset) {
        assetMapper.update(asset);
    }

    @Transactional
    public void delete(String assetId, String userId) {
        assetMapper.delete(assetId, userId);
    }

    @Transactional
    public void adjustBalance(String assetId, String userId, BigDecimal newBalance, String remark) {
        Asset asset = assetMapper.queryById(assetId);
        if (asset == null || !asset.getUserId().equals(userId)) {
            return;
        }

        BigDecimal beforeBalance = asset.getBalance();
        BigDecimal changeAmount = newBalance.subtract(beforeBalance);

        asset.setBalance(newBalance);
        assetMapper.update(asset);

        AssetRecord record = AssetRecord.builder()
                .recordId(IDUtil.recordId())
                .assetId(assetId)
                .userId(userId)
                .operationType(AssetRecord.OP_MANUAL_ADJUST)
                .beforeBalance(beforeBalance)
                .afterBalance(newBalance)
                .changeAmount(changeAmount)
                .remark(remark != null ? remark : "手动调整余额")
                .build();
        assetMapper.insertRecord(record);
    }

    public List<AssetRecord> queryRecords(String assetId) {
        return assetMapper.queryRecords(assetId);
    }

    public Map<String, BigDecimal> getOverview(String userId) {
        List<Asset> assets = assetMapper.queryList(userId);
        
        BigDecimal totalAsset = BigDecimal.ZERO;
        BigDecimal totalLiability = BigDecimal.ZERO;
        
        for (Asset asset : assets) {
            if (asset.isLiability()) {
                totalLiability = totalLiability.add(asset.getBalance().abs());
            } else {
                totalAsset = totalAsset.add(asset.getBalance());
            }
        }
        
        BigDecimal netAsset = totalAsset.subtract(totalLiability);
        
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalAsset", totalAsset);
        result.put("totalLiability", totalLiability);
        result.put("netAsset", netAsset);
        return result;
    }

    public List<Map<String, Object>> getYearlyTrend(String userId, int year, String type) {
        List<Asset> allAssets = assetMapper.queryList(userId);
        List<Asset> filteredAssets = filterAssetsByType(allAssets, type);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("label", String.format("%02d月", month));
            
            if (year == currentYear && month > currentMonth) {
                monthData.put("value", BigDecimal.ZERO);
                trend.add(monthData);
                continue;
            }
            
            LocalDateTime monthEnd = LocalDate.of(year, month, 1)
                    .plusMonths(1)
                    .atStartOfDay();
            String endTime = monthEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            BigDecimal totalValue = BigDecimal.ZERO;
            
            for (Asset asset : filteredAssets) {
                BigDecimal monthBalance = getAssetBalanceAtTime(asset, endTime);
                totalValue = totalValue.add(monthBalance);
            }
            
            monthData.put("value", totalValue);
            trend.add(monthData);
        }
        
        return trend;
    }

    private List<Asset> filterAssetsByType(List<Asset> assets, String type) {
        switch (type) {
            case "asset":
                return assets.stream().filter(a -> !a.isLiability()).toList();
            case "liability":
                return assets.stream().filter(Asset::isLiability).toList();
            case "netAsset":
                return assets;
            default:
                return assets;
        }
    }

    private BigDecimal getAssetBalanceAtTime(Asset asset, String endTime) {
        if (asset.getCreateTime() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalDateTime assetCreateTime = asset.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime queryTime = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        if (assetCreateTime.isAfter(queryTime)) {
            return BigDecimal.ZERO;
        }
        
        AssetRecord lastRecord = assetMapper.queryLastRecordBeforeTime(asset.getAssetId(), endTime);
        
        if (lastRecord != null) {
            return lastRecord.getAfterBalance();
        }
        
        return asset.getBalance();
    }
}
