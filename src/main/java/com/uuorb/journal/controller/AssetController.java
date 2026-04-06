package com.uuorb.journal.controller;

import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.Log;
import com.uuorb.journal.annotation.UserId;
import com.uuorb.journal.constant.ResultStatus;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.model.Asset;
import com.uuorb.journal.model.AssetRecord;
import com.uuorb.journal.service.AssetService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/asset")
public class AssetController {

    @Resource
    AssetService assetService;

    @Log
    @Authorization
    @GetMapping("/overview")
    Result getOverview(@UserId String userId) {
        Map<String, BigDecimal> overview = assetService.getOverview(userId);
        return Result.ok(overview);
    }

    @Log
    @Authorization
    @GetMapping("/list")
    Result getList(@UserId String userId) {
        List<Asset> assets = assetService.queryList(userId);
        return Result.ok(assets);
    }

    @Log
    @Authorization
    @GetMapping("/{assetId}")
    Result getById(@PathVariable("assetId") String assetId, @UserId String userId) {
        Asset asset = assetService.queryById(assetId);
        if (asset == null || !asset.getUserId().equals(userId)) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        return Result.ok(asset);
    }

    @Log
    @Authorization
    @PostMapping
    Result create(@RequestBody Asset asset, @UserId String userId) {
        asset.setUserId(userId);
        Asset created = assetService.create(asset);
        return Result.ok(created);
    }

    @Log
    @Authorization
    @PutMapping("/{assetId}")
    Result update(@PathVariable("assetId") String assetId, @RequestBody Asset asset, @UserId String userId) {
        Asset existing = assetService.queryById(assetId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        asset.setAssetId(assetId);
        asset.setUserId(userId);
        assetService.update(asset);
        return Result.ok();
    }

    @Log
    @Authorization
    @DeleteMapping("/{assetId}")
    Result delete(@PathVariable("assetId") String assetId, @UserId String userId) {
        Asset existing = assetService.queryById(assetId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        assetService.delete(assetId, userId);
        return Result.ok();
    }

    @Log
    @Authorization
    @PostMapping("/{assetId}/adjust")
    Result adjustBalance(
            @PathVariable("assetId") String assetId,
            @RequestBody Map<String, Object> params,
            @UserId String userId) {
        
        Asset existing = assetService.queryById(assetId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        
        BigDecimal newBalance = new BigDecimal(params.get("balance").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        
        assetService.adjustBalance(assetId, userId, newBalance, remark);
        return Result.ok();
    }

    @Log
    @Authorization
    @GetMapping("/{assetId}/records")
    Result getRecords(@PathVariable("assetId") String assetId, @UserId String userId) {
        Asset existing = assetService.queryById(assetId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        List<AssetRecord> records = assetService.queryRecords(assetId);
        return Result.ok(records);
    }
}
