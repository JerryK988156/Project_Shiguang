package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.CheckinRecordMapper;
import com.example.shiguang.mapper.GoalMapper;
import com.example.shiguang.mapper.UserMapper;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final GoalMapper goalMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final AuthService authService;

    public AdminService(UserMapper userMapper, GoalMapper goalMapper, CheckinRecordMapper checkinRecordMapper, AuthService authService) {
        this.userMapper = userMapper;
        this.goalMapper = goalMapper;
        this.checkinRecordMapper = checkinRecordMapper;
        this.authService = authService;
    }

    public Map<String, Object> overview() {
        SessionUtils.requireAdmin();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("goalCount", goalMapper.selectCount(null));
        result.put("checkinCount", checkinRecordMapper.selectCount(null));
        result.put("activeUserCount7d", checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                        .ge(CheckinRecord::getCheckinDate, LocalDate.now().minusDays(6)))
                .stream()
                .map(CheckinRecord::getUserId)
                .distinct()
                .count());
        return result;
    }

    public List<Map<String, Object>> userList() {
        SessionUtils.requireAdmin();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime, User::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> item = new LinkedHashMap<>(authService.toSafeUser(user));
            item.put("goalCount", goalMapper.selectCount(new LambdaQueryWrapper<Goal>()
                    .eq(Goal::getUserId, user.getId())));
            item.put("checkinCount", checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getUserId, user.getId())));
            result.add(item);
        }
        return result;
    }
}
