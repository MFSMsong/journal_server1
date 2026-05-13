package com.uuorb.journal.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.Log;
import com.uuorb.journal.controller.vo.ChartsAggregateResult;
import com.uuorb.journal.controller.vo.ChartsDataNode;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.mapper.ChartsMapper;
import com.uuorb.journal.model.Expense;
import com.uuorb.journal.service.ExpenseService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/charts")
public class ChartsController {

    @Resource
    ChartsMapper chartsMapper;
    @Resource
    ExpenseService expenseService;

    public static List<String> daysOfWeek() {
        LocalDate today = LocalDate.now();
        DayOfWeek currentDay = today.getDayOfWeek();
        List<String> daysOfWeek = new ArrayList<>(Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));

        int todayIndex = currentDay.getValue() - 1;

        List<String> nextDays = new ArrayList<>();
        for (int i = 0; i < daysOfWeek.size(); i++) {
            int dayIndex = (todayIndex + i + 1) % daysOfWeek.size();
            nextDays.add(daysOfWeek.get(dayIndex));
        }

        return nextDays;
    }

    @Log
    @Authorization
    @GetMapping("/weekly/{activityId}")
    Result getWeeklyCharts(@PathVariable("activityId") String activityId) {
        getInfo(activityId);
        List<String> DAYS_OF_WEEK = daysOfWeek();

        List<ChartsDataNode> chartsDataNodes = chartsMapper.queryWeekly(activityId);

        if (chartsDataNodes.size() == 0) {
            return Result.ok();
        }

        for (String day : DAYS_OF_WEEK) {
            if (!chartsDataNodes.stream().anyMatch(e -> day.equals(e.getName()))) {
                ChartsDataNode newDay = new ChartsDataNode();
                newDay.setName(day);
                newDay.setValue(BigDecimal.ZERO);
                chartsDataNodes.add(newDay);
            }
        }

        chartsDataNodes.sort(Comparator.comparing(e -> DAYS_OF_WEEK.indexOf(e.getName())));
        return Result.ok(chartsDataNodes);
    }

    private static void getInfo(String activityId) {
        log.info("查看可视化：activity:{}", activityId);
    }

    @Log
    @Authorization
    @GetMapping("/weekly/income/{activityId}")
    Result getWeeklyChartsIncome(@PathVariable("activityId") String activityId) {
        getInfo(activityId);
        List<String> DAYS_OF_WEEK = daysOfWeek();

        List<ChartsDataNode> chartsDataNodes = chartsMapper.queryWeeklyIncome(activityId);
        if (chartsDataNodes.isEmpty()) {
            return Result.ok();
        }
        for (String day : DAYS_OF_WEEK) {
            if (!chartsDataNodes.stream().anyMatch(e -> day.equals(e.getName()))) {
                ChartsDataNode newDay = new ChartsDataNode();
                newDay.setName(day);
                newDay.setValue(BigDecimal.ZERO);
                chartsDataNodes.add(newDay);
            }
        }

        chartsDataNodes.sort(Comparator.comparing(e -> DAYS_OF_WEEK.indexOf(e.getName())));
        return Result.ok(chartsDataNodes);
    }

    @Log
    @Authorization
    @GetMapping("/weekly/type/{activityId}")
    Result getWeeklyChartsGroupByType(@PathVariable("activityId") String activityId) {
        getInfo(activityId);

        List<ChartsDataNode> chartsDataNodes = chartsMapper.queryGroupByType(activityId);

        return Result.ok(chartsDataNodes);
    }

    @Log
    @Authorization
    @GetMapping("/aggregate/{activityId}")
    Result getAggregate(
            @PathVariable("activityId") String activityId,
            @RequestParam("period") String period,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "typeMode", defaultValue = "expense") String typeMode) {

        log.info("聚合统计：activity={}, period={}, year={}, month={}, startDate={}, typeMode={}",
                activityId, period, year, month, startDate, typeMode);

        List<ChartsDataNode> expenses;
        List<ChartsDataNode> income;
        List<ChartsDataNode> types;

        boolean queryIncome = "income".equals(typeMode);

        switch (period) {
            case "week":
                if (startDate == null) {
                    LocalDate today = LocalDate.now();
                    DayOfWeek weekday = today.getDayOfWeek();
                    startDate = today.minusDays(weekday.getValue() - 1);
                }
                LocalDate endDate = startDate.plusDays(7);
                String startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                expenses = fillWeekData(chartsMapper.queryByWeek(activityId, startStr, endStr, false), startDate);
                income = fillWeekData(chartsMapper.queryByWeek(activityId, startStr, endStr, true), startDate);
                types = chartsMapper.queryTypeByWeek(activityId, startStr, endStr, queryIncome);
                break;

            case "month":
                if (year == null) year = LocalDate.now().getYear();
                if (month == null) month = LocalDate.now().getMonthValue();

                int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
                expenses = fillMonthData(chartsMapper.queryByMonth(activityId, year, month, false), daysInMonth);
                income = fillMonthData(chartsMapper.queryByMonth(activityId, year, month, true), daysInMonth);
                types = chartsMapper.queryTypeByMonth(activityId, year, month, queryIncome);
                break;

            case "year":
                if (year == null) year = LocalDate.now().getYear();

                expenses = fillYearData(chartsMapper.queryByYear(activityId, year, false));
                income = fillYearData(chartsMapper.queryByYear(activityId, year, true));
                types = chartsMapper.queryTypeByYear(activityId, year, queryIncome);
                break;

            default:
                return Result.error(-1, "不支持的统计周期: " + period);
        }

        return Result.ok(new ChartsAggregateResult(expenses, income, types));
    }

    private List<ChartsDataNode> fillWeekData(List<ChartsDataNode> data, LocalDate startDate) {
        Map<String, BigDecimal> dataMap = new HashMap<>();
        for (ChartsDataNode node : data) {
            dataMap.put(node.getName(), node.getValue());
        }

        List<ChartsDataNode> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String label = date.format(formatter);
            BigDecimal value = dataMap.getOrDefault(label, BigDecimal.ZERO);
            ChartsDataNode node = new ChartsDataNode();
            node.setName(label);
            node.setValue(value);
            result.add(node);
        }
        return result;
    }

    private List<ChartsDataNode> fillMonthData(List<ChartsDataNode> data, int daysInMonth) {
        Map<String, BigDecimal> dataMap = new HashMap<>();
        for (ChartsDataNode node : data) {
            dataMap.put(node.getName(), node.getValue());
        }

        List<ChartsDataNode> result = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            String label = String.format("%02d", i);
            BigDecimal value = dataMap.getOrDefault(label, BigDecimal.ZERO);
            ChartsDataNode node = new ChartsDataNode();
            node.setName(label);
            node.setValue(value);
            result.add(node);
        }
        return result;
    }

    private List<ChartsDataNode> fillYearData(List<ChartsDataNode> data) {
        Map<String, BigDecimal> dataMap = new HashMap<>();
        for (ChartsDataNode node : data) {
            dataMap.put(node.getName(), node.getValue());
        }

        List<ChartsDataNode> result = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            String label = i + "月";
            BigDecimal value = dataMap.getOrDefault(label, BigDecimal.ZERO);
            ChartsDataNode node = new ChartsDataNode();
            node.setName(label);
            node.setValue(value);
            result.add(node);
        }
        return result;
    }

    @Log
    @Authorization
    @GetMapping("/export/{activityId}")
    public void exportExcel(HttpServletResponse response, @PathVariable("activityId") String activityId) throws IOException {
        List list = expenseService.queryListUnlimited(activityId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("测试", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Expense.class).sheet("模板").doWrite(list);
    }

    @Log
    @GetMapping("/test")
    public String test() {
        List list = expenseService.queryListUnlimited("aca120b534f3b04eb8");
        return JSON.toJSONString(list);
    }
}
