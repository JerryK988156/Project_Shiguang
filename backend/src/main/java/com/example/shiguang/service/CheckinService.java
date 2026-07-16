package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.CheckinRecordMapper;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.dto.CheckinDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckinService {
    private final CheckinRecordMapper checkinRecordMapper;
    private final GoalService goalService;

    public CheckinService(CheckinRecordMapper checkinRecordMapper, GoalService goalService) {
        this.checkinRecordMapper = checkinRecordMapper;
        this.goalService = goalService;
    }

    public CheckinRecord add(CheckinDTO dto) {
        if (dto == null || dto.getGoalId() == null) {
            throw new BusinessException("目标不能为空");
        }
        Goal goal = goalService.requireOwnedGoal(dto.getGoalId());
        LocalDate checkinDate = dto.getCheckinDate() == null ? LocalDate.now() : dto.getCheckinDate();
        long exists = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, SessionUtils.requireUserId())
                .eq(CheckinRecord::getGoalId, goal.getId())
                .eq(CheckinRecord::getCheckinDate, checkinDate));
        if (exists > 0) {
            throw new BusinessException("同一任务每天只能打卡一次");
        }

        CheckinRecord record = new CheckinRecord();
        record.setUserId(SessionUtils.requireUserId());
        record.setGoalId(goal.getId());
        record.setCheckinDate(checkinDate);
        record.setStudyDuration(dto.getStudyDuration() == null ? 0 : dto.getStudyDuration());
        record.setContent(dto.getContent());
        record.setRemark(dto.getRemark());
        checkinRecordMapper.insert(record);
        return checkinRecordMapper.selectById(record.getId());
    }

    public List<CheckinRecord> list(Long goalId) {
        LambdaQueryWrapper<CheckinRecord> wrapper = new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, SessionUtils.requireUserId());
        if (goalId != null) {
            goalService.requireOwnedGoal(goalId);
            wrapper.eq(CheckinRecord::getGoalId, goalId);
        }
        wrapper.orderByDesc(CheckinRecord::getCheckinDate, CheckinRecord::getId);
        return checkinRecordMapper.selectList(wrapper);
    }

    public Map<String, Object> today(Long goalId) {
        List<CheckinRecord> todayRecords = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, SessionUtils.requireUserId())
                .eq(CheckinRecord::getCheckinDate, LocalDate.now())
                .orderByDesc(CheckinRecord::getId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now());
        result.put("records", todayRecords);
        result.put("goalIds", todayRecords.stream().map(CheckinRecord::getGoalId).collect(Collectors.toList()));
        if (goalId != null) {
            goalService.requireOwnedGoal(goalId);
            result.put("hasChecked", todayRecords.stream().anyMatch(item -> item.getGoalId().equals(goalId)));
        }
        return result;
    }

    public void delete(Long id) {
        CheckinRecord record = requireOwnedRecord(id);
        checkinRecordMapper.deleteById(record.getId());
    }

    private CheckinRecord requireOwnedRecord(Long id) {
        CheckinRecord record = checkinRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(SessionUtils.requireUserId())) {
            throw new BusinessException("打卡记录不存在");
        }
        return record;
    }
}
