package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.dto.UpdatePasswordDTO;
import com.example.shiguang.model.dto.UpdateProfileDTO;
import com.example.shiguang.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/profile")
    public JsonResponse<Map<String, Object>> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return JsonResponse.success(userService.updateProfile(dto), "修改资料成功");
    }

    @PostMapping("/password")
    public JsonResponse<Void> updatePassword(@RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(dto);
        return JsonResponse.success(null, "修改密码成功");
    }
}
