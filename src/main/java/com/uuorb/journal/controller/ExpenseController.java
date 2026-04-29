package com.uuorb.journal.controller;

import com.github.pagehelper.PageInfo;
import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.Log;
import com.uuorb.journal.annotation.UserId;
import com.uuorb.journal.constant.ResultStatus;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.model.Activity;
import com.uuorb.journal.model.Expense;
import com.uuorb.journal.model.User;
import com.uuorb.journal.service.ActivityService;
import com.uuorb.journal.service.ExpenseService;
import com.uuorb.journal.service.UserService;
import com.uuorb.journal.service.WebSocketService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Resource
    UserService userService;

    @Resource
    ExpenseService expenseService;

    @Resource
    ActivityService activityService;
    
    @Resource
    WebSocketService webSocketService;

    @Log
    @DeleteMapping("{expenseId}/{activityId}")
    Result deleteExpense(@PathVariable("expenseId") String expenseId, 
                         @PathVariable("activityId") String activityId,
                         @UserId String userId) {
        Activity query = Activity.builder().activityId(activityId).userId(userId).build();
        boolean isOwner = activityService.isOwnerActivity(query);
        
        Expense expense = expenseService.queryById(expenseId);
        if (expense == null) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }
        
        if (!isOwner) {
            boolean hasPermission = activityService.hasQueryPermission(query);
            if (!hasPermission) {
                return Result.error(ResultStatus.NOT_OWN_RESOURCE);
            }
            if (!expense.getUserId().equals(userId)) {
                return Result.error(ResultStatus.NOT_OWN_RESOURCE);
            }
        }

        expenseService.deleteExpense(expenseId, activityId);
        
        webSocketService.notifyExpenseDelete(activityId, expenseId, userId);

        return Result.ok();
    }


    /**
     * 获取自己的账单列表
     * @param activityId: 账本id
     * @param pageNum: 分页页码
     * @param userId: 用户ID，从header中取jwt解析
     * @return 账单列表
     */
    @Log
    @GetMapping("/list/{activityId}")
    Result<PageInfo<Expense>> getActivityExpenseList(
            @PathVariable("activityId") String activityId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @UserId String userId) {

        Activity query = Activity.builder().activityId(activityId).userId(userId).build();

        // 判断有没有查询权限
        boolean hasPermission = activityService.hasQueryPermission(query);
        if (!hasPermission) {
            return Result.error(ResultStatus.NOT_OWN_RESOURCE);
        }

        if (pageNum == null) {
            pageNum = 1;
        }

        // 查询
        PageInfo<Expense> pageInfo = expenseService.queryList(activityId, pageNum);

        return Result.ok(pageInfo);
    }

    @Authorization
    @Log
    @PostMapping
    Result insert(@RequestBody Expense expense, @UserId String userId) {
        expense.setUserId(userId);
        
        Activity query = Activity.builder()
                .activityId(expense.getActivityId())
                .userId(userId)
                .build();
        
        boolean hasPermission = activityService.hasQueryPermission(query);
        if (!hasPermission) {
            return Result.error(ResultStatus.NOT_OWN_RESOURCE);
        }
        
        expenseService.insertExpenseAndCalcRemainingBudget(expense);
        
        webSocketService.notifyExpenseAdd(expense.getActivityId(), expense, userId);
        
        return Result.ok(expense);
    }

    @Authorization
    @Log
    @PostMapping("/current")
    Result insertCurrent(@RequestBody Expense expense, @UserId String userId) {
        expense.setUserId(userId);

        User user = userService.getUserByUserId(userId);
        String currentActivityId = user.getCurrentActivityId();

        if (currentActivityId == null) {
            return Result.error(ResultStatus.NOT_OWN_RESOURCE);
        }
        
        Activity query = Activity.builder()
                .activityId(currentActivityId)
                .userId(userId)
                .build();
        
        boolean hasPermission = activityService.hasQueryPermission(query);
        if (!hasPermission) {
            return Result.error(ResultStatus.NOT_OWN_RESOURCE);
        }
        
        expense.setActivityId(currentActivityId);
        expenseService.insertExpenseAndCalcRemainingBudget(expense);
        
        webSocketService.notifyExpenseAdd(currentActivityId, expense, userId);

        return Result.ok(expense);
    }

    @Log
    @PatchMapping
    Result update(@RequestBody Expense expense, @UserId String userId) {
        String expenseId = expense.getExpenseId();
        String activityId = expense.getActivityId();
        // 1. 判断是否有expenseId和activityId
        if (expenseId == null || expense.getActivityId() == null) {
            return Result.error(ResultStatus.PRIMARY_ID_MISS);
        }

        // 2. 判断活动是否存在
        Activity activity = activityService.queryActivityByActivityId(activityId);
        if (activity == null) {
            return Result.error(ResultStatus.RESOURCE_NOT_FOUND);
        }

        // 3. 判断有没有查询权限
        expense.setUserId(userId);

        Activity query = Activity.builder().activityId(expense.getActivityId()).userId(userId).build();
        boolean hasPermission = activityService.hasQueryPermission(query);
        if (!hasPermission) {
            return Result.error(ResultStatus.NOT_OWN_RESOURCE);
        }
        // 4. 判断expense是否是自己的
        // todo: 这个应该是配置项，先放开了
        //        try {
        //            expenseService.checkIsOwner(expense);
        //        } catch (CustomException e) {
        //            return Result.error(e.getCustomErrEnum());
        //        }

        expenseService.update(expense);
        
        webSocketService.notifyExpenseUpdate(expense.getActivityId(), expense, userId);

        return Result.ok();
    }

    @Authorization
    @GetMapping("/search")
    Result<List<Expense>> searchGlobal(
            @RequestParam("keyword") String keyword,
            @UserId String userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Expense> results = expenseService.searchGlobal(userId, keyword.trim());
        return Result.ok(results);
    }
}
