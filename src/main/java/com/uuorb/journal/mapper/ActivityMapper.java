package com.uuorb.journal.mapper;

import com.uuorb.journal.model.Activity;
import com.uuorb.journal.model.ActivityMember;
import com.uuorb.journal.model.ActivityUserRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ActivityMapper {
    List<Activity> querySelfActivityList(Activity activity);
    List<Activity> queryJoinedActivityList(Activity activity);

    void insert(Activity activity);

    void updateAllInActive(@Param("activityId") String id, @Param("userId") String userId);

    Integer relCount(ActivityUserRel query);

    Boolean isOwner(Activity query);

    Integer updateActivity(Activity activity);

    Activity queryActivityByActivityId(String activityId);

    void joinActivity(ActivityUserRel joinQuery);

    boolean isJoinedOwner(Activity activity);

    void refreshActivityRemainingBudget(String activityId);

    Activity getCurrentActivity(@Param("userId") String userId);

    @Delete("DELETE FROM activity_user_rel WHERE activity_id = #{activityId}")
    void deleteAllRef(String activityId);

    @Delete("DELETE FROM activity WHERE activity_id = #{activityId}")
    void deleteActivity(String activityId);

    @Update("UPDATE  users SET current_activity_id = null WHERE current_activity_id = #{activityId}")
    void deleteAllUserActivityRef(String activityId);


    @Delete("DELETE FROM activity_user_rel WHERE activity_id = #{activityId} AND user_id = #{userId}")
    void exitActivity(String activityId, String userId);

    List<ActivityMember> queryActivityMembers(String activityId);

    void updateMemberNickname(ActivityUserRel activityUserRel);

    void kickMember(String activityId, String userId);

    @Delete("DELETE FROM activity_user_rel WHERE user_id = #{userId}")
    void deleteUserRel(String userId);

    @Delete("DELETE FROM activity WHERE user_id = #{userId}")
    void deleteActivityByUserId(String userId);

    @org.apache.ibatis.annotations.Select("SELECT nickname FROM activity_user_rel WHERE activity_id = #{activityId} AND user_id = #{userId}")
    String getMemberNickname(@Param("activityId") String activityId, @Param("userId") String userId);

    @org.apache.ibatis.annotations.Select("SELECT u.user_id as userId, u.avatar, IFNULL(aur.nickname, u.nickname) as nickname FROM activity_user_rel aur LEFT JOIN users u ON aur.user_id = u.user_id WHERE aur.activity_id = #{activityId} AND aur.user_id = #{userId}")
    ActivityMember getMemberInfo(@Param("activityId") String activityId, @Param("userId") String userId);
}
