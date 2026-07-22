package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "智能助手", description = "AI 智能体对话接口")
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "智能对话")
    @PostMapping("/chat")
    public JsonResponse<Map<String, String>> chat(@RequestBody Map<String, Object> body) {
        Long userId = SessionUtils.requireUserId();
        String sessionId = body.get("sessionId") instanceof String s && !s.isBlank()
                ? s : UUID.randomUUID().toString();

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

        String reply = agentService.chat(userId, sessionId, messages);
        return JsonResponse.success(Map.of("reply", reply, "sessionId", sessionId), "对话成功");
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "智能对话（流式）")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        Long userId = SessionUtils.requireUserId();
        String sessionId = body.get("sessionId") instanceof String s && !s.isBlank()
                ? s : UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(60000L);

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
                emitter.completeWithError(new IllegalArgumentException("消息不能为空"));
                return emitter;
            }
            messages = List.of(Map.of("role", "user", "content", message));
        }

        if (messages.isEmpty()) {
            emitter.completeWithError(new IllegalArgumentException("消息不能为空"));
            return emitter;
        }

        final List<Map<String, String>> finalMessages = messages;
        new Thread(() -> {
            try {
                agentService.chatStream(userId, sessionId, finalMessages, token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        // emitter might be closed
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @Operation(summary = "获取聊天历史")
    @GetMapping("/history")
    public JsonResponse<List<Map<String, String>>> history(@RequestParam String sessionId) {
        Long userId = SessionUtils.requireUserId();
        return JsonResponse.success(agentService.loadHistory(userId, sessionId));
    }
}
