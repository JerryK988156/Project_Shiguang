package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.dto.GoalDTO;
import com.example.shiguang.service.GoalService;
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

    @PutMapping("/update")
    public JsonResponse<Goal> update(@RequestBody GoalDTO dto) {
        return JsonResponse.success(goalService.update(dto), "修改目标成功");
    }

    @DeleteMapping("/delete/{id}")
    public JsonResponse<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return JsonResponse.success(null, "删除目标成功");
    }

    @GetMapping("/detail/{id}")
    public JsonResponse<Goal> detail(@PathVariable Long id) {
        return JsonResponse.success(goalService.detail(id));
    }

    @GetMapping("/list")
    public JsonResponse<List<Goal>> list(@RequestParam(required = false) String status) {
        return JsonResponse.success(goalService.list(status));
    }

    @PutMapping("/status/{id}")
    public JsonResponse<Goal> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return JsonResponse.success(goalService.updateStatus(id, status), "更新状态成功");
    }
}
