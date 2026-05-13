package com.uuorb.journal.mapper;

import com.uuorb.journal.controller.vo.ChartsDataNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChartsMapper {

    List<ChartsDataNode> queryWeekly(String activityId);
    List<ChartsDataNode> queryWeeklyIncome(String activityId);

    List<ChartsDataNode> queryGroupByType(String activityId);

    List<ChartsDataNode> queryByWeek(@Param("activityId") String activityId,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate,
                                     @Param("isExpense") boolean isExpense);

    List<ChartsDataNode> queryByMonth(@Param("activityId") String activityId,
                                      @Param("year") int year,
                                      @Param("month") int month,
                                      @Param("isExpense") boolean isExpense);

    List<ChartsDataNode> queryByYear(@Param("activityId") String activityId,
                                     @Param("year") int year,
                                     @Param("isExpense") boolean isExpense);

    List<ChartsDataNode> queryTypeByWeek(@Param("activityId") String activityId,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("isExpense") boolean isExpense);

    List<ChartsDataNode> queryTypeByMonth(@Param("activityId") String activityId,
                                          @Param("year") int year,
                                          @Param("month") int month,
                                          @Param("isExpense") boolean isExpense);

    List<ChartsDataNode> queryTypeByYear(@Param("activityId") String activityId,
                                         @Param("year") int year,
                                         @Param("isExpense") boolean isExpense);

}
