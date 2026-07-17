package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.dto.UpdatePasswordDTO;
import com.example.shiguang.model.dto.UpdateProfileDTO;
import com.example.shiguang.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path resolveUploadDir() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        return dir.toPath();
    }

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

    @PostMapping("/avatar")
    public JsonResponse<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return JsonResponse.error("请选择图片文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return JsonResponse.error("仅支持 jpg、jpeg、png、gif、webp 格式");
        }

        Path avatarDir = resolveUploadDir().resolve("avatars");
        Files.createDirectories(avatarDir);

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = avatarDir.resolve(filename);
        file.transferTo(targetPath.toFile());

        String avatarUrl = "/uploads/avatars/" + filename;
        return JsonResponse.success(userService.updateAvatar(avatarUrl), "头像上传成功");
    }
}
