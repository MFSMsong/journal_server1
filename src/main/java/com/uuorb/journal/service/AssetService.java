package com.uuorb.journal.service;

import com.uuorb.journal.mapper.AssetMapper;
import com.uuorb.journal.model.Asset;
import com.uuorb.journal.model.AssetRecord;
import com.uuorb.journal.util.IDUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
}
