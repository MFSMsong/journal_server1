package com.uuorb.journal.controller.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChartsAggregateResult {
    private List<ChartsDataNode> expenses;
    private List<ChartsDataNode> income;
    private List<ChartsDataNode> types;

    public ChartsAggregateResult() {
    }

    public ChartsAggregateResult(List<ChartsDataNode> expenses, List<ChartsDataNode> income, List<ChartsDataNode> types) {
        this.expenses = expenses;
        this.income = income;
        this.types = types;
    }
}
