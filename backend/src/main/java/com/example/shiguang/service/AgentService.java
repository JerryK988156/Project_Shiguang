package com.example.shiguang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shiguang.common.config.AiConfig;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.AgentMessageMapper;
import com.example.shiguang.model.domain.AgentMessage;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.dto.CheckinDTO;
import com.example.shiguang.model.dto.GoalDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final RestClient llmRestClient;
    private final ObjectMapper objectMapper;
    private final GoalService goalService;
    private final CheckinService checkinService;
    private final StatService statService;
    private final AgentMessageMapper agentMessageMapper;
    private final String model;

    public AgentService(RestClient llmRestClient, ObjectMapper objectMapper,
                        GoalService goalService, CheckinService checkinService,
                        StatService statService, AiConfig aiConfig,
                        AgentMessageMapper agentMessageMapper) {
        this.llmRestClient = llmRestClient;
        this.objectMapper = objectMapper;
        this.goalService = goalService;
        this.checkinService = checkinService;
        this.statService = statService;
        this.agentMessageMapper = agentMessageMapper;
        this.model = aiConfig.getModel();
    }

    private static final String SYSTEM_PROMPT = """
        你是"拾光计划"学习打卡系统的智能助手。你可以使用函数调用来帮助用户管理系统。
        规则：
        - 如果用户想打卡但没有指定目标，先调用 listGoals 再调用 addCheckin
        - 日期格式 yyyy-MM-dd，今天是2026-07-21
        - addCheckin 的 minutes 是学习分钟数（2小时=120分钟）
        - 回复时使用友好、鼓励的语气
        - 调用 listGoals 拿到目标列表后，根据用户意图匹配最相关的目标进行打卡
        - 回复格式使用纯文本，不要使用 Markdown 表格、不要使用 ## 标题、不要使用 ** 加粗、不要使用 --- 分隔线
        - 列表使用 - 开头即可，每行一条
        """;

    private static final String TOOLS_JSON = """
        [
          {
            "type": "function",
            "function": {
              "name": "addGoal",
              "description": "创建新的学习目标",
              "parameters": {
                "type": "object",
                "properties": {
                  "title": {"type": "string", "description": "目标标题"},
                  "description": {"type": "string", "description": "目标描述，可为空"},
                  "startDate": {"type": "string", "description": "开始日期 yyyy-MM-dd"},
                  "endDate": {"type": "string", "description": "结束日期 yyyy-MM-dd"},
                  "tags": {"type": "array", "items": {"type": "string"}, "description": "标签列表"}
                },
                "required": ["title", "startDate", "endDate"]
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "addCheckin",
              "description": "为指定目标新增打卡记录，日期为今天",
              "parameters": {
                "type": "object",
                "properties": {
                  "goalId": {"type": "integer", "description": "目标ID"},
                  "minutes": {"type": "integer", "description": "学习时长（分钟）"},
                  "note": {"type": "string", "description": "打卡备注，可为空"}
                },
                "required": ["goalId", "minutes"]
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "listGoals",
              "description": "获取用户的目标列表，可按状态筛选",
              "parameters": {
                "type": "object",
                "properties": {
                  "status": {"type": "string", "description": "状态筛选：进行中/已完成/已放弃，不传则返回全部"}
                },
                "required": []
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "getStatsOverview",
              "description": "获取用户统计概览（总目标数、总打卡数、总时长、连续天数）",
              "parameters": {"type": "object", "properties": {}, "required": []}
            }
          },
          {
            "type": "function",
            "function": {
              "name": "getTrend",
              "description": "获取近N天的打卡趋势数据",
              "parameters": {
                "type": "object",
                "properties": {
                  "days": {"type": "integer", "description": "天数，7或30"}
                },
                "required": ["days"]
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "getCheckinCalendar",
              "description": "获取用户的打卡日历热力图数据",
              "parameters": {"type": "object", "properties": {}, "required": []}
            }
          },
          {
            "type": "function",
            "function": {
              "name": "listCheckins",
              "description": "获取打卡记录列表，可按目标筛选",
              "parameters": {
                "type": "object",
                "properties": {
                  "goalId": {"type": "integer", "description": "目标ID，不传则返回全部"}
                },
                "required": []
              }
            }
          },
          {
            "type": "function",
            "function": {
              "name": "updateGoalStatus",
              "description": "修改目标状态",
              "parameters": {
                "type": "object",
                "properties": {
                  "goalId": {"type": "integer", "description": "目标ID"},
                  "status": {"type": "string", "description": "新状态：已完成/已放弃/进行中"}
                },
                "required": ["goalId", "status"]
              }
            }
          }
        ]
        """;

    /**
     * 标准 function calling 流程：发送 tools，循环处理 tool_calls 直至完成。
     */
    public String chat(Long userId, String sessionId, List<Map<String, String>> history) {
        try {
            // 保存用户消息
            if (history != null && !history.isEmpty()) {
                Map<String, String> lastUserMsg = null;
                for (int i = history.size() - 1; i >= 0; i--) {
                    if ("user".equals(history.get(i).get("role"))) {
                        lastUserMsg = history.get(i);
                        break;
                    }
                }
                if (lastUserMsg != null) {
                    saveMessage(userId, sessionId, "user", lastUserMsg.get("content"));
                }
            }

            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            for (Map<String, String> msg : history) {
                messages.addObject().put("role", msg.get("role")).put("content", msg.get("content"));
            }

            for (int round = 0; round < 5; round++) {
                JsonNode response = callLlm(messages);
                String content = response.path("choices").path(0).path("message").path("content").asText("");
                String finishReason = response.path("choices").path(0).path("finish_reason").asText("");

                if ("tool_calls".equals(finishReason)) {
                    JsonNode toolCalls = response.path("choices").path(0).path("message").path("tool_calls");

                    // 添加 assistant 消息
                    ObjectNode asst = messages.addObject();
                    asst.put("role", "assistant");
                    if (content != null && !content.isBlank()) asst.put("content", content);
                    asst.set("tool_calls", toolCalls);

                    // 执行并添加 tool 结果
                    for (JsonNode tc : toolCalls) {
                        String callId = tc.path("id").asText();
                        String funcName = tc.path("function").path("name").asText();
                        String argsStr = tc.path("function").path("arguments").asText();
                        String result;
                        try { result = executeFunction(funcName, argsStr); }
                        catch (Exception e) { result = "操作失败：" + e.getMessage(); }
                        log.debug("Agent called {} ({}) => {}", funcName, argsStr, result);

                        ObjectNode tool = messages.addObject();
                        tool.put("role", "tool");
                        tool.put("tool_call_id", callId);
                        tool.put("content", result);
                    }
                    continue;
                }

                String finalReply = content.isBlank() ? "请告诉我你需要什么帮助？" : stripEmoji(cleanMarkers(content));
                saveMessage(userId, sessionId, "assistant", finalReply);
                return finalReply;
            }
            String fallback = "抱歉，操作次数过多，请简化你的请求。";
            saveMessage(userId, sessionId, "assistant", fallback);
            return fallback;
        } catch (Exception e) {
            return "抱歉，智能助手暂时无法处理你的请求，请稍后重试。";
        }
    }

    /** 保存消息到数据库 */
    public void saveMessage(Long userId, String sessionId, String role, String content) {
        AgentMessage msg = new AgentMessage();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.now());
        agentMessageMapper.insert(msg);
    }

    /** 加载聊天历史，最近 50 条 */
    public List<Map<String, String>> loadHistory(Long userId, String sessionId) {
        List<AgentMessage> messages = agentMessageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getUserId, userId)
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByAsc(AgentMessage::getCreateTime)
                        .last("limit 50"));
        List<Map<String, String>> result = new ArrayList<>();
        for (AgentMessage msg : messages) {
            Map<String, String> item = new HashMap<>();
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            result.add(item);
        }
        return result;
    }

    /** 发送 messages + tools 到 LLM */
    private JsonNode callLlm(ArrayNode messages) throws JsonProcessingException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.set("tools", objectMapper.readTree(TOOLS_JSON));

        String response = llmRestClient.post()
                .uri("/v1/chat/completions")
                .body(body.toString())
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(response);
    }

    // ── 函数调用解析 ──

    private record FuncCall(String name, String args) {}
    private record ParseResult(String visibleText, java.util.List<FuncCall> calls) {
        boolean hasCalls() { return calls != null && !calls.isEmpty(); }
    }

    /**
     * 从 LLM 返回的 content 中解析函数调用。
     * 同时支持：
     * 1. DeepSeek 原生格式：<｜tool_calls▁begin｜><｜tool_call▁begin｜>name<｜tool_call▁separator▁begin｜>json<｜tool_call▁end｜><｜tool_calls▁end｜>
     * 2. 自定义格式：<!--FUNC:name {"json"}-->
     */
    private ParseResult parseFunctionCalls(String content) {
        java.util.List<FuncCall> calls = new java.util.ArrayList<>();

        // 尝试 DeepSeek 原生格式
        String dsBegin = "<｜tool_calls▁begin｜>";
        int dsIdx = content.indexOf(dsBegin);
        if (dsIdx >= 0) {
            String visible = content.substring(0, dsIdx).trim();
            String toolSection = content.substring(dsIdx);

            // 匹配每个 tool_call 块: <｜tool_call▁begin｜>name<|tool_call_separator_begin|>args<｜tool_call▁end｜>
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<｜tool_call▁begin｜>(.+?)(?:<｜tool_call▁separator▁begin｜>(.+?))?<｜tool_call▁end｜>"
            );
            java.util.regex.Matcher m = p.matcher(toolSection);
            while (m.find()) {
                String name = m.group(1).trim();
                String args = m.group(2) != null ? m.group(2).trim() : "{}";
                calls.add(new FuncCall(name, args));
            }
            return new ParseResult(visible, calls);
        }

        // 尝试自定义 <!--CALL:name args--> 或 <!--FUNC:name args--> 格式
        String funcMarker = null;
        int markerIdx = -1;
        for (String m : new String[]{"<!--CALL:", "<!--FUNC:"}) {
            int idx = content.indexOf(m);
            if (idx >= 0 && (markerIdx < 0 || idx < markerIdx)) {
                markerIdx = idx;
                funcMarker = m;
            }
        }
        if (markerIdx >= 0) {
            int markerEnd = content.indexOf("-->", markerIdx);
            if (markerEnd > markerIdx) {
                String visible = content.substring(0, markerIdx).trim();
                String funcCall = content.substring(markerIdx + funcMarker.length(), markerEnd).trim();
                int spaceIdx = funcCall.indexOf(' ');
                if (spaceIdx > 0) {
                    calls.add(new FuncCall(funcCall.substring(0, spaceIdx), funcCall.substring(spaceIdx + 1)));
                } else {
                    calls.add(new FuncCall(funcCall, "{}"));
                }
                return new ParseResult(visible, calls);
            }
        }

        return new ParseResult(content, calls);
    }

    /** 清理残留的 DeepSeek 特殊标记 */
    private String cleanMarkers(String text) {
        if (text == null) return "";
        return text.replaceAll("<｜tool_calls▁begin｜>.*", "")
                   .replaceAll("<｜tool_call▁begin｜>.*", "")
                   .replaceAll("<｜tool_call▁end｜>", "")
                   .replaceAll("<｜tool_calls▁end｜>", "")
                   .replaceAll("<｜tool_call▁separator▁begin｜>.*", "")
                   .trim();
    }

    /** 移除渲染乱码 + Markdown→纯文本转换 */
    private String stripEmoji(String text) {
        if (text == null) return "";
        String cleaned = text;
        // 移除所有连续 ? 字符（Unicode 渲染失败产物）
        cleaned = cleaned.replaceAll("\\?{2,}", "");
        // 移除不可打印的控制字符和私有区字符（保留中文、ASCII、常用标点）
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        // Markdown → 纯文本转换
        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s+", "");      // ## 标题
        cleaned = cleaned.replaceAll("\\*\\*(.+?)\\*\\*", "$1");  // **加粗**
        cleaned = cleaned.replaceAll("(?m)^---+$", "");            // --- 分隔线
        cleaned = cleaned.replaceAll("\\|", " ");                  // 表格 |
        // 合并多余空行
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.trim();
    }

    /**
     * 真正的流式对话 — 调用 LLM API 时设置 stream: true，逐 token 回调
     */
    public void chatStream(Long userId, String sessionId, List<Map<String, String>> history, java.util.function.Consumer<String> onToken) {
        try {
            // 保存用户消息
            if (history != null && !history.isEmpty()) {
                Map<String, String> lastUserMsg = null;
                for (int i = history.size() - 1; i >= 0; i--) {
                    if ("user".equals(history.get(i).get("role"))) {
                        lastUserMsg = history.get(i);
                        break;
                    }
                }
                if (lastUserMsg != null) {
                    saveMessage(userId, sessionId, "user", lastUserMsg.get("content"));
                }
            }

            ArrayNode messages = objectMapper.createArrayNode();
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            for (Map<String, String> msg : history) {
                messages.addObject().put("role", msg.get("role")).put("content", msg.get("content"));
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.set("tools", objectMapper.readTree(TOOLS_JSON));
            body.put("stream", true);

            StringBuilder fullReply = new StringBuilder();
            
            llmRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(body.toString())
                    .exchange((request, response) -> {
                        try (var reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) break;
                                    try {
                                        JsonNode chunk = objectMapper.readTree(data);
                                        String delta = chunk.path("choices").path(0).path("delta").path("content").asText("");
                                        if (!delta.isEmpty()) {
                                            fullReply.append(delta);
                                            onToken.accept(delta);
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        return null;
                    });

            String finalReply = fullReply.toString();
            if (!finalReply.isBlank()) {
                String cleaned = stripEmoji(cleanMarkers(finalReply));
                saveMessage(userId, sessionId, "assistant", cleaned);
            } else {
                // 流式无文本返回（LLM 触发了 function calling），回退到非流式 chat()
                String fallbackReply = chat(userId, sessionId, history);
                for (int i = 0; i < fallbackReply.length(); i++) {
                    onToken.accept(String.valueOf(fallbackReply.charAt(i)));
                }
            }
        } catch (Exception e) {
            log.error("Agent stream error", e);
            onToken.accept("[抱歉，智能助手暂时无法处理你的请求]");
        }
    }

    private JsonNode callLlm(List<Map<String, String>> history) throws JsonProcessingException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        ArrayNode messages = body.putArray("messages");

        // System message
        ObjectNode sysMsg = messages.addObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);

        // 对话历史
        for (Map<String, String> msg : history) {
            ObjectNode historyMsg = messages.addObject();
            historyMsg.put("role", msg.get("role"));
            historyMsg.put("content", msg.get("content"));
        }

        // 发送 tools 定义（DeepSeek 使用原生文本格式输出，但需要知道可用函数）
        try {
            JsonNode toolsNode = objectMapper.readTree(TOOLS_JSON);
            body.set("tools", toolsNode);
        } catch (JsonProcessingException ignored) {}

        String response = llmRestClient.post()
                .uri("/v1/chat/completions")
                .body(body.toString())
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(response);
    }

    private String executeFunction(String funcName, String argsStr) throws JsonProcessingException {
        JsonNode args = objectMapper.readTree(argsStr);

        return switch (funcName) {
            case "addGoal" -> {
                GoalDTO dto = new GoalDTO();
                dto.setTitle(args.path("title").asText());
                dto.setDescription(args.path("description").asText(""));
                LocalDate start = LocalDate.parse(args.path("startDate").asText());
                LocalDate end = LocalDate.parse(args.path("endDate").asText());
                dto.setStartDate(start);
                dto.setEndDate(end);
                dto.setTargetDays((int) (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1));
                if (args.has("tags")) {
                    List<String> tags = objectMapper.convertValue(
                            args.get("tags"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    dto.setTags(tags);
                }
                Goal goal = goalService.add(dto);
                yield "目标「" + goal.getTitle() + "」创建成功，ID：" + goal.getId()
                        + "，时间：" + goal.getStartDate() + " 至 " + goal.getEndDate();
            }
            case "addCheckin" -> {
                CheckinDTO dto = new CheckinDTO();
                dto.setGoalId(args.path("goalId").asLong());
                dto.setStudyDuration(args.path("minutes").asInt());
                dto.setContent(args.path("note").asText(""));
                Map<String, Object> result = checkinService.add(dto);
                CheckinRecord record = (CheckinRecord) result.get("record");
                yield "打卡成功！记录ID：" + record.getId() + "，学习 " + record.getStudyDuration() + " 分钟";
            }
            case "listGoals" -> {
                String status = args.has("status") && !args.path("status").isNull()
                        ? args.path("status").asText() : null;
                List<Goal> goals = goalService.list(status);
                if (goals.isEmpty()) {
                    yield "你还没有任何目标，试试让我帮你创建一个吧！";
                }
                StringBuilder sb = new StringBuilder("你的目标列表：\n");
                for (Goal g : goals) {
                    sb.append("- [").append(g.getId()).append("] ").append(g.getTitle())
                            .append("（").append(g.getStatus()).append("）\n");
                }
                yield sb.toString();
            }
            case "getStatsOverview" -> {
                Map<String, Object> overview = statService.overview();
                yield "学习概览：\n- 目标总数：" + overview.get("goalCount")
                        + "\n- 累计打卡：" + overview.get("totalCheckinCount") + " 次"
                        + "\n- 累计时长：" + overview.get("totalMinutes") + " 分钟"
                        + "\n- 连续打卡：" + overview.get("currentStreak") + " 天";
            }
            case "getTrend" -> {
                int days = args.path("days").asInt();
                List<Map<String, Object>> trend = days == 30 ? statService.trend30() : statService.trend7();
                if (trend.isEmpty()) {
                    yield "暂无打卡趋势数据";
                }
                StringBuilder sb = new StringBuilder("近" + days + "天打卡趋势：\n");
                for (Map<String, Object> item : trend) {
                    sb.append("- ").append(item.get("date")).append("：打卡 ")
                            .append(item.get("count")).append(" 次，学习 ")
                            .append(item.get("minutes")).append(" 分钟\n");
                }
                yield sb.toString();
            }
            case "getCheckinCalendar" -> {
                Map<String, Object> calendar = statService.checkinCalendar();
                if (calendar == null || calendar.isEmpty()) {
                    yield "暂无打卡日历数据";
                }
                List<?> dataList = (List<?>) calendar.get("data");
                if (dataList == null || dataList.isEmpty()) {
                    yield "暂无打卡记录，开始你的第一次打卡吧！";
                }
                yield "已获取打卡日历数据，共有 " + dataList.size() + " 天有打卡记录";
            }
            case "listCheckins" -> {
                Long goalId = args.has("goalId") && !args.path("goalId").isNull()
                        ? args.path("goalId").asLong() : null;
                List<CheckinRecord> checkins = checkinService.list(goalId);
                if (checkins.isEmpty()) {
                    yield "暂无打卡记录";
                }
                StringBuilder sb = new StringBuilder("打卡记录：\n");
                int limit = Math.min(checkins.size(), 20);
                for (int i = 0; i < limit; i++) {
                    CheckinRecord c = checkins.get(i);
                    sb.append("- ").append(c.getCheckinDate()).append("：")
                            .append(c.getStudyDuration()).append(" 分钟");
                    if (c.getContent() != null && !c.getContent().isEmpty()) {
                        sb.append("（").append(c.getContent()).append("）");
                    }
                    sb.append("\n");
                }
                if (checkins.size() > 20) {
                    sb.append("... 共 ").append(checkins.size()).append(" 条记录");
                }
                yield sb.toString();
            }
            case "updateGoalStatus" -> {
                Long goalId = args.path("goalId").asLong();
                String status = args.path("status").asText();
                Goal goal = goalService.updateStatus(goalId, status);
                yield "目标「" + goal.getTitle() + "」状态已更新为「" + goal.getStatus() + "」";
            }
            default -> "未知操作：" + funcName;
        };
    }
}
