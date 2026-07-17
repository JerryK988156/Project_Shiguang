package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.utls.PasswordUtils;
import com.example.shiguang.mapper.UserMapper;
import com.example.shiguang.model.domain.User;
import com.example.shiguang.model.dto.UpdatePasswordDTO;
import com.example.shiguang.model.dto.UpdateProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final AuthService authService;

    public UserService(UserMapper userMapper, AuthService authService) {
        this.userMapper = userMapper;
        this.authService = authService;
    }

    public Map<String, Object> updateProfile(UpdateProfileDTO dto) {
        User user = authService.requireCurrentUserEntity();

        if (dto != null && StringUtils.hasText(dto.getUsername())) {
            String newUsername = dto.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                // username 唯一性校验
                User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, newUsername)
                        .last("limit 1"));
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new BusinessException("账号 " + newUsername + " 已被占用，请更换");
                }
                user.setUsername(newUsername);
            }
        }

        user.setNickname(dto != null ? dto.getNickname() : user.getNickname());
        user.setAvatar(dto != null ? dto.getAvatar() : user.getAvatar());
        user.setProfile(dto != null ? dto.getProfile() : user.getProfile());
        userMapper.updateById(user);
        return authService.toSafeUser(userMapper.selectById(user.getId()));
    }

    public void updatePassword(UpdatePasswordDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getOldPassword()) || !StringUtils.hasText(dto.getNewPassword())) {
            throw new BusinessException("旧密码和新密码不能为空");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new BusinessException("新密码长度不能少于 6 位");
        }

        User user = authService.requireCurrentUserEntity();
        if (!PasswordUtils.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(PasswordUtils.md5(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    public Map<String, Object> updateAvatar(String avatarUrl) {
        User user = authService.requireCurrentUserEntity();
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
        return authService.toSafeUser(userMapper.selectById(user.getId()));
    }
}
