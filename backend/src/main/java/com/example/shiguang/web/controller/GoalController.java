package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.common.annotation.Auditable;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.dto.GoalDTO;
import com.example.shiguang.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "目标管理", description = "学习目标增删改查接口")
@RestController
@RequestMapping("/api/goal")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("/add")
    public JsonResponse<Goal> add(@RequestBody GoalDTO dto) {
        return JsonResponse.success(goalService.add(dto), "新增目标成功");
    }

    @Operation(summary = "修改目标")
    @PutMapping("/update")
    public JsonResponse<Goal> update(@RequestBody GoalDTO dto) {
        return JsonResponse.success(goalService.update(dto), "修改目标成功");
    }

    @Auditable(action = "删除目标", target = "#id")
    @Operation(summary = "删除目标")
    @DeleteMapping("/delete/{id}")
    public JsonResponse<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return JsonResponse.success(null, "删除目标成功");
    }

    @Operation(summary = "获取目标详情")
    @GetMapping("/detail/{id}")
    public JsonResponse<Goal> detail(@PathVariable Long id) {
        return JsonResponse.success(goalService.detail(id));
    }

    @Operation(summary = "获取目标列表")
    @GetMapping("/list")
    public JsonResponse<List<Goal>> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        return JsonResponse.success(goalService.list(status, keyword));
    }

    @Auditable(action = "修改目标状态")
    @Operation(summary = "修改目标状态")
    @PutMapping("/status/{id}")
    public JsonResponse<Goal> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return JsonResponse.success(goalService.updateStatus(id, status), "更新状态成功");
    }
}
