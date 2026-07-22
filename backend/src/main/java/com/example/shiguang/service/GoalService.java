package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.GoalMapper;
import com.example.shiguang.mapper.GoalTagMapper;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.domain.GoalTag;
import com.example.shiguang.model.dto.GoalDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class GoalService {
    private final GoalMapper goalMapper;
    private final GoalTagMapper goalTagMapper;

    public GoalService(GoalMapper goalMapper, GoalTagMapper goalTagMapper) {
        this.goalMapper = goalMapper;
        this.goalTagMapper = goalTagMapper;
    }

    public Goal add(GoalDTO dto) {
        validate(dto);
        Goal goal = new Goal();
        goal.setUserId(SessionUtils.requireUserId());
        goal.setTitle(dto.getTitle());
        goal.setDescription(dto.getDescription());
        goal.setStartDate(dto.getStartDate());
        goal.setEndDate(dto.getEndDate());
        goal.setTargetDays(dto.getTargetDays());
        goal.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "进行中");
        goalMapper.insert(goal);

        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            for (String tagName : dto.getTags()) {
                if (StringUtils.hasText(tagName)) {
                    GoalTag goalTag = new GoalTag();
                    goalTag.setGoalId(goal.getId());
                    goalTag.setTagName(tagName.trim());
                    goalTagMapper.insert(goalTag);
                }
            }
        }

        return fillTags(goalMapper.selectById(goal.getId()));
    }

    public Goal update(GoalDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("目标 id 不能为空");
        }
        validate(dto);
        Goal goal = requireOwnedGoal(dto.getId());
        goal.setTitle(dto.getTitle());
        goal.setDescription(dto.getDescription());
        goal.setStartDate(dto.getStartDate());
        goal.setEndDate(dto.getEndDate());
        goal.setTargetDays(dto.getTargetDays());
        if (StringUtils.hasText(dto.getStatus())) {
            goal.setStatus(dto.getStatus());
        }
        goalMapper.updateById(goal);

        if (dto.getTags() != null) {
            goalTagMapper.delete(new LambdaQueryWrapper<GoalTag>()
                    .eq(GoalTag::getGoalId, goal.getId()));
            for (String tagName : dto.getTags()) {
                if (StringUtils.hasText(tagName)) {
                    GoalTag goalTag = new GoalTag();
                    goalTag.setGoalId(goal.getId());
                    goalTag.setTagName(tagName.trim());
                    goalTagMapper.insert(goalTag);
                }
            }
        }

        return fillTags(goalMapper.selectById(goal.getId()));
    }

    @CacheEvict(value = {"statOverview","statTrend7","statTrend30","statGoalProgress","statCalendar","statTagStats","statWeeklyReport","statTimeDist"}, key = "T(com.example.shiguang.common.utls.SessionUtils).requireUserId()")
    public void delete(Long id) {
        Goal goal = requireOwnedGoal(id);
        goalMapper.deleteById(goal.getId());
    }

    public Goal detail(Long id) {
        return fillTags(requireOwnedGoal(id));
    }

    public List<Goal> list(String status) {
        return list(status, null);
    }

    public List<Goal> list(String status, String keyword) {
        LambdaQueryWrapper<Goal> wrapper = new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, SessionUtils.requireUserId())
                .orderByDesc(Goal::getCreateTime);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Goal::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Goal::getTitle, keyword)
                    .or()
                    .exists("SELECT 1 FROM goal_tag WHERE goal_tag.goal_id = goal.id AND goal_tag.tag_name LIKE {0}",
                            "%" + keyword + "%"));
        }
        List<Goal> goals = goalMapper.selectList(wrapper);
        for (Goal goal : goals) {
            fillTags(goal);
        }
        return goals;
    }

    public Goal updateStatus(Long id, String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException("状态不能为空");
        }
        Goal goal = requireOwnedGoal(id);
        goal.setStatus(status);
        goalMapper.updateById(goal);
        return fillTags(goalMapper.selectById(goal.getId()));
    }

    public Goal requireOwnedGoal(Long id) {
        if (id == null) {
            throw new BusinessException("目标 id 不能为空");
        }
        Goal goal = goalMapper.selectById(id);
        if (goal == null || !goal.getUserId().equals(SessionUtils.requireUserId())) {
            throw new BusinessException("目标不存在");
        }
        return goal;
    }

    private Goal fillTags(Goal goal) {
        if (goal == null) {
            return null;
        }
        List<GoalTag> tagList = goalTagMapper.selectList(new LambdaQueryWrapper<GoalTag>()
                .eq(GoalTag::getGoalId, goal.getId()));
        List<String> tags = tagList.stream()
                .map(GoalTag::getTagName)
                .toList();
        goal.setTags(tags);
        return goal;
    }

    private void validate(GoalDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("目标标题不能为空");
        }
        if (dto.getStartDate() == null) {
            throw new BusinessException("开始日期不能为空");
        }
    }
}
