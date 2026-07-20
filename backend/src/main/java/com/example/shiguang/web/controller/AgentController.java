package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.service.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chat")
    public JsonResponse<Map<String, String>> chat(@RequestBody Map<String, Object> body) {
        // 支持两种格式：旧的 {message:"..."} 和新的 {messages:[{role,content},...]}
        List<Map<String, String>> messages;

        if (body.containsKey("messages") && body.get("messages") instanceof List<?> list) {
            messages = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    String role = m.get("role") instanceof String s ? s : "";
                    String content = m.get("content") instanceof String s ? s : "";
                    if (!role.isEmpty() && !content.isEmpty()) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }
        } else {
            String message = body.get("message") instanceof String s ? s : "";
            if (message.isBlank()) {
                return JsonResponse.error("消息不能为空");
            }
            messages = List.of(Map.of("role", "user", "content", message));
        }

        if (messages.isEmpty()) {
            return JsonResponse.error("消息不能为空");
        }

        String reply = agentService.chat(messages);
        return JsonResponse.success(Map.of("reply", reply), "对话成功");
    }
}
