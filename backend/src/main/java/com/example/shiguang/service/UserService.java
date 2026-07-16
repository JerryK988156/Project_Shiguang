package com.example.shiguang.service;

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
        user.setNickname(dto == null ? null : dto.getNickname());
        user.setAvatar(dto == null ? null : dto.getAvatar());
        user.setProfile(dto == null ? null : dto.getProfile());
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
}
