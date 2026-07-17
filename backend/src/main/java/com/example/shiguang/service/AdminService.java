package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.BusinessException;
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
        result.put("isSuperAdmin", SessionUtils.isSuperAdmin());
        return result;
    }

    public List<Map<String, Object>> userList() {
        SessionUtils.requireAdmin();
        Long currentUserId = SessionUtils.requireUserId();
        boolean isSuperAdmin = SessionUtils.isSuperAdmin();

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> item = new LinkedHashMap<>(authService.toSafeUser(user));
            item.put("goalCount", goalMapper.selectCount(new LambdaQueryWrapper<Goal>()
                    .eq(Goal::getUserId, user.getId())));
            item.put("checkinCount", checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getUserId, user.getId())));

            // 判断当前管理员能否修改此用户状态
            boolean isSelf = user.getId().equals(currentUserId);
            boolean canModify = !isSelf && (isSuperAdmin || !"admin".equals(user.getRole()));
            item.put("canModify", canModify);
            item.put("isSelf", isSelf);
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> toggleUserStatus(Long id, Integer status) {
        SessionUtils.requireAdmin();
        if (id == null) {
            throw new BusinessException("用户 id 不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（禁用）或 1（正常）");
        }

        User targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能修改自己的状态
        Long currentUserId = SessionUtils.requireUserId();
        if (currentUserId.equals(id)) {
            throw new BusinessException("不能修改自己的账号状态");
        }

        // 普通管理员不能修改其他管理员的权限
        if ("admin".equals(targetUser.getRole()) && !SessionUtils.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可以修改其他管理员的权限");
        }

        targetUser.setStatus(status);
        userMapper.updateById(targetUser);
        return authService.toSafeUser(userMapper.selectById(id));
    }
}
