package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.dto.CheckinDTO;
import com.example.shiguang.service.CheckinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {
    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/add")
    public JsonResponse<CheckinRecord> add(@RequestBody CheckinDTO dto) {
        return JsonResponse.success(checkinService.add(dto), "打卡成功");
    }

    @GetMapping("/list")
    public JsonResponse<List<CheckinRecord>> list(@RequestParam(required = false) Long goalId) {
        return JsonResponse.success(checkinService.list(goalId));
    }

    @GetMapping("/today")
    public JsonResponse<Map<String, Object>> today(@RequestParam(required = false) Long goalId) {
        return JsonResponse.success(checkinService.today(goalId));
    }

    @PostMapping("/delete/{id}")
    public JsonResponse<Void> delete(@PathVariable Long id) {
        checkinService.delete(id);
        return JsonResponse.success(null, "删除打卡成功");
    }
}
