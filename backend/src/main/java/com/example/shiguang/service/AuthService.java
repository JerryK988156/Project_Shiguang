package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.utls.PasswordUtils;
import com.example.shiguang.common.utls.JwtUtils;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.UserMapper;
import com.example.shiguang.model.domain.User;
import com.example.shiguang.model.dto.LoginDTO;
import com.example.shiguang.model.dto.RegisterDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Map<String, Object> login(LoginDTO loginDTO) {
        if (loginDTO == null || !StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new BusinessException("用户名和密码不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .last("limit 1"));
        if (user == null || !PasswordUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        SessionUtils.login(user);
        Map<String, Object> result = toSafeUser(user);
        result.put("token", JwtUtils.generateToken(user.getId(), user.getRole()));
        return result;
    }

    public Map<String, Object> register(RegisterDTO registerDTO) {
        if (registerDTO == null || !StringUtils.hasText(registerDTO.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!registerDTO.getUsername().matches("^[A-Za-z0-9_]{4,20}$")) {
            throw new BusinessException("用户名须为4-20位字母、数字或下划线");
        }
        if (!StringUtils.hasText(registerDTO.getPassword()) || registerDTO.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }

        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
                .last("limit 1"));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(PasswordUtils.md5(registerDTO.getPassword()));
        user.setNickname(StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);

        SessionUtils.login(user);
        Map<String, Object> result = toSafeUser(user);
        result.put("token", JwtUtils.generateToken(user.getId(), user.getRole()));
        return result;
    }

    public void logout() {
        SessionUtils.logout();
    }

    public Map<String, Object> currentUser() {
        Long userId = SessionUtils.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return toSafeUser(user);
    }

    public User requireCurrentUserEntity() {
        Long userId = SessionUtils.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }

    public Map<String, Object> toSafeUser(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        result.put("avatar", user.getAvatar());
        result.put("profile", user.getProfile());
        result.put("status", user.getStatus());
        result.put("createTime", user.getCreateTime());
        result.put("updateTime", user.getUpdateTime());
        return result;
    }
}
