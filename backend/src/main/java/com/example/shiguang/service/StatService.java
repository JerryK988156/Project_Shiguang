package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.CheckinRecordMapper;
import com.example.shiguang.mapper.GoalMapper;
import com.example.shiguang.mapper.GoalTagMapper;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.domain.GoalTag;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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
    private final GoalTagMapper goalTagMapper;

    public StatService(GoalMapper goalMapper, CheckinRecordMapper checkinRecordMapper, GoalTagMapper goalTagMapper) {
        this.goalMapper = goalMapper;
        this.checkinRecordMapper = checkinRecordMapper;
        this.goalTagMapper = goalTagMapper;
    }

    @Cacheable(value = "statOverview", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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

    @Cacheable(value = "statTrend7", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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

    @Cacheable(value = "statGoalProgress", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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

    @Cacheable(value = "statTrend30", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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

    @Cacheable(value = "statTimeDist", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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

            List<GoalTag> tagList = goalTagMapper.selectList(new LambdaQueryWrapper<GoalTag>()
                    .eq(GoalTag::getGoalId, goal.getId()));
            List<String> tags = tagList.stream().map(GoalTag::getTagName).toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goalId", goal.getId());
            item.put("title", goal.getTitle());
            item.put("totalMinutes", totalMinutes);
            item.put("tags", tags);
            result.add(item);
        }
        return result;
    }

    @Cacheable(value = "statCalendar", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
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
        long activeGoalCount = goalMapper.selectCount(new LambdaQueryWrapper<Goal>().eq(Goal::getUserId, userId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("maxCount", Math.max(1, maxCount));
        result.put("maxPossible", Math.max(1, activeGoalCount));
        return result;
    }

    @Cacheable(value = "statTagStats", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
    public List<Map<String, Object>> tagStats() {
        Long userId = SessionUtils.requireUserId();
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId));

        Map<String, Integer> tagMinutes = new LinkedHashMap<>();
        Map<String, Integer> tagCount = new LinkedHashMap<>();

        for (Goal goal : goals) {
            List<GoalTag> tagList = goalTagMapper.selectList(new LambdaQueryWrapper<GoalTag>()
                    .eq(GoalTag::getGoalId, goal.getId()));
            List<String> tags = tagList.stream().map(GoalTag::getTagName).toList();

            List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getUserId, userId)
                    .eq(CheckinRecord::getGoalId, goal.getId()));
            int totalMinutes = records.stream().mapToInt(item -> item.getStudyDuration() == null ? 0 : item.getStudyDuration()).sum();

            for (String tag : tags) {
                tagMinutes.merge(tag, totalMinutes, Integer::sum);
                tagCount.merge(tag, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String tag : tagMinutes.keySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tagName", tag);
            item.put("totalMinutes", tagMinutes.getOrDefault(tag, 0));
            item.put("goalCount", tagCount.getOrDefault(tag, 0));
            result.add(item);
        }
        return result;
    }

    @Cacheable(value = "statWeeklyReport", key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
    public Map<String, Object> weeklyReport() {
        Long userId = SessionUtils.requireUserId();
        LocalDate now = LocalDate.now();
        LocalDate thisWeekStart = now.with(DayOfWeek.MONDAY);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        List<CheckinRecord> allRecords = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, lastWeekStart)
                .orderByAsc(CheckinRecord::getCheckinDate));

        List<CheckinRecord> thisWeekRecords = allRecords.stream()
                .filter(r -> !r.getCheckinDate().isBefore(thisWeekStart))
                .toList();
        List<CheckinRecord> lastWeekRecords = allRecords.stream()
                .filter(r -> !r.getCheckinDate().isBefore(lastWeekStart) && r.getCheckinDate().isBefore(thisWeekStart))
                .toList();

        Map<String, Object> thisWeek = buildWeekStat(thisWeekRecords);
        Map<String, Object> lastWeek = buildWeekStat(lastWeekRecords);

        long twDays = (long) thisWeek.get("totalDays");
        long twMinutes = (long) thisWeek.get("totalMinutes");
        long twGoals = (long) thisWeek.get("checkedInGoals");
        long lwDays = (long) lastWeek.get("totalDays");
        long lwMinutes = (long) lastWeek.get("totalMinutes");
        long lwGoals = (long) lastWeek.get("checkedInGoals");

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("daysPct", calcPct(twDays, lwDays));
        changes.put("minutesPct", calcPct(twMinutes, lwMinutes));
        changes.put("goalsPct", calcPct(twGoals, lwGoals));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("thisWeek", thisWeek);
        result.put("lastWeek", lastWeek);
        result.put("changes", changes);
        return result;
    }

    private Map<String, Object> buildWeekStat(List<CheckinRecord> records) {
        long totalDays = records.stream().map(CheckinRecord::getCheckinDate).distinct().count();
        long totalMinutes = records.stream().mapToInt(r -> r.getStudyDuration() == null ? 0 : r.getStudyDuration()).sum();
        long checkedInGoals = records.stream().map(CheckinRecord::getGoalId).distinct().count();
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("totalDays", totalDays);
        stat.put("totalMinutes", totalMinutes);
        stat.put("checkedInGoals", checkedInGoals);
        return stat;
    }

    private static Double calcPct(double thisVal, double lastVal) {
        if (lastVal == 0) return null;
        return Math.round((thisVal - lastVal) / lastVal * 1000.0) / 10.0;
    }
}
