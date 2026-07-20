package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.AchievementMapper;
import com.example.shiguang.mapper.CheckinRecordMapper;
import com.example.shiguang.mapper.GoalMapper;
import com.example.shiguang.model.domain.Achievement;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AchievementService {
    private final AchievementMapper achievementMapper;
    private final GoalMapper goalMapper;
    private final CheckinRecordMapper checkinRecordMapper;

    public AchievementService(AchievementMapper achievementMapper, GoalMapper goalMapper, CheckinRecordMapper checkinRecordMapper) {
        this.achievementMapper = achievementMapper;
        this.goalMapper = goalMapper;
        this.checkinRecordMapper = checkinRecordMapper;
    }

    private static final LinkedHashMap<Integer, String> MILESTONES = new LinkedHashMap<>();

    static {
        MILESTONES.put(7, "青铜🥉");
        MILESTONES.put(14, "白银🥈");
        MILESTONES.put(21, "黄金🥇");
        MILESTONES.put(30, "钻石💎");
        MILESTONES.put(60, "大师🏆");
        MILESTONES.put(100, "传奇👑");
    }

    public Map<String, Object> checkAndAward(Long goalId) {
        Goal goal = goalMapper.selectById(goalId);
        if (goal == null) {
            return null;
        }

        long checkedDays = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getGoalId, goalId)
                .eq(CheckinRecord::getUserId, SessionUtils.requireUserId()));

        for (Map.Entry<Integer, String> entry : MILESTONES.entrySet()) {
            int milestoneDays = entry.getKey();
            if (checkedDays >= milestoneDays) {
                Long existing = achievementMapper.selectCount(new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getUserId, SessionUtils.requireUserId())
                        .eq(Achievement::getGoalId, goalId)
                        .eq(Achievement::getMilestoneDays, milestoneDays));
                if (existing == 0) {
                    Achievement achievement = new Achievement();
                    achievement.setUserId(SessionUtils.requireUserId());
                    achievement.setGoalId(goalId);
                    achievement.setGoalTitle(goal.getTitle());
                    achievement.setMilestoneDays(milestoneDays);
                    achievement.setBadgeName(entry.getValue());
                    achievement.setAchievedDate(LocalDate.now());
                    achievementMapper.insert(achievement);

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("earned", true);
                    result.put("milestoneDays", milestoneDays);
                    result.put("badgeName", entry.getValue());
                    result.put("goalTitle", goal.getTitle());
                    return result;
                }
            }
        }
        return null;
    }

    public List<Achievement> listByUser() {
        return achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getUserId, SessionUtils.requireUserId())
                .orderByDesc(Achievement::getAchievedDate));
    }
}
