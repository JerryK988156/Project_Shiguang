package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.CheckinRecordMapper;
import com.example.shiguang.mapper.GoalMapper;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatService {
    private final GoalMapper goalMapper;
    private final CheckinRecordMapper checkinRecordMapper;

    public StatService(GoalMapper goalMapper, CheckinRecordMapper checkinRecordMapper) {
        this.goalMapper = goalMapper;
        this.checkinRecordMapper = checkinRecordMapper;
    }

    public Map<String, Object> overview() {
        Long userId = SessionUtils.requireUserId();
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId));
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .orderByDesc(CheckinRecord::getCheckinDate));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalCount", goals.size());
        result.put("activeGoalCount", goals.stream().filter(item -> "进行中".equals(item.getStatus())).count());
        result.put("completedGoalCount", goals.stream().filter(item -> "已完成".equals(item.getStatus())).count());
        result.put("totalCheckinCount", records.size());
        result.put("totalMinutes", records.stream().mapToInt(item -> item.getStudyDuration() == null ? 0 : item.getStudyDuration()).sum());
        result.put("todayCheckinCount", records.stream().filter(item -> LocalDate.now().equals(item.getCheckinDate())).count());
        result.put("currentStreak", currentStreak(records));
        return result;
    }

    public List<Map<String, Object>> trend7() {
        Long userId = SessionUtils.requireUserId();
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, LocalDate.now().minusDays(6))
                .orderByAsc(CheckinRecord::getCheckinDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<CheckinRecord> dayRecords = records.stream()
                    .filter(item -> date.equals(item.getCheckinDate()))
                    .toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", dayRecords.size());
            item.put("minutes", dayRecords.stream().mapToInt(record -> record.getStudyDuration() == null ? 0 : record.getStudyDuration()).sum());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> goalProgress() {
        Long userId = SessionUtils.requireUserId();
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId)
                .orderByDesc(Goal::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Goal goal : goals) {
            long checkedDays = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getUserId, userId)
                    .eq(CheckinRecord::getGoalId, goal.getId()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goalId", goal.getId());
            item.put("title", goal.getTitle());
            item.put("status", goal.getStatus());
            item.put("targetDays", goal.getTargetDays());
            item.put("checkedDays", checkedDays);
            item.put("progressPct", calculateProgress(goal.getTargetDays(), checkedDays));
            result.add(item);
        }
        return result;
    }

    private long currentStreak(List<CheckinRecord> records) {
        Set<LocalDate> dates = records.stream()
                .map(CheckinRecord::getCheckinDate)
                .collect(Collectors.toSet());
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = records.getFirst().getCheckinDate();
        long streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minus(1, ChronoUnit.DAYS);
        }
        return streak;
    }

    private double calculateProgress(Integer targetDays, long checkedDays) {
        if (targetDays == null || targetDays <= 0) {
            return 0D;
        }
        return Math.round((checkedDays * 1000.0 / targetDays)) / 10.0;
    }

    public List<Map<String, Object>> trend30() {
        return trend(30);
    }

    private List<Map<String, Object>> trend(int days) {
        Long userId = SessionUtils.requireUserId();
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, LocalDate.now().minusDays(days - 1))
                .orderByAsc(CheckinRecord::getCheckinDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<CheckinRecord> dayRecords = records.stream()
                    .filter(item -> date.equals(item.getCheckinDate()))
                    .toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", dayRecords.size());
            item.put("minutes", dayRecords.stream().mapToInt(record -> record.getStudyDuration() == null ? 0 : record.getStudyDuration()).sum());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> goalTimeDistribution() {
        Long userId = SessionUtils.requireUserId();
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Goal goal : goals) {
            List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getUserId, userId)
                    .eq(CheckinRecord::getGoalId, goal.getId()));
            int totalMinutes = records.stream().mapToInt(item -> item.getStudyDuration() == null ? 0 : item.getStudyDuration()).sum();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goalId", goal.getId());
            item.put("title", goal.getTitle());
            item.put("totalMinutes", totalMinutes);
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> checkinCalendar() {
        Long userId = SessionUtils.requireUserId();
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .orderByAsc(CheckinRecord::getCheckinDate));

        Map<String, Long> dateCounts = records.stream()
                .collect(Collectors.groupingBy(r -> r.getCheckinDate().toString(), LinkedHashMap::new, Collectors.counting()));

        List<List<Object>> data = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dateCounts.entrySet()) {
            List<Object> pair = new ArrayList<>();
            pair.add(entry.getKey());
            pair.add(entry.getValue());
            data.add(pair);
        }

        long maxCount = dateCounts.values().stream().max(Long::compare).orElse(1L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("maxCount", Math.max(1, maxCount));
        return result;
    }
}
