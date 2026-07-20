package com.example.shiguang.service;

import com.example.shiguang.common.config.AiConfig;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.model.domain.CheckinRecord;
import com.example.shiguang.model.domain.Goal;
import com.example.shiguang.model.dto.CheckinDTO;
import com.example.shiguang.model.dto.GoalDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private final RestClient llmRestClient;
    private final ObjectMapper objectMapper;
    private final GoalService goalService;
    private final CheckinService checkinService;
    private final StatService statService;
    private final String model;

    public AgentService(RestClient llmRestClient, ObjectMapper objectMapper,
                        GoalService goalService, CheckinService checkinService,
                        StatService statService, AiConfig aiConfig) {
        this.llmRestClient = llmRestClient;
        this.objectMapper = objectMapper;
        this.goalService = goalService;
        this.checkinService = checkinService;
        this.statService = statService;
        this.model = aiConfig.getModel();
    }

    private static final String SYSTEM_PROMPT = """
        你是"拾光计划"学习打卡系统的智能助手。你可以自由地与用户对话，同时也能帮助用户管理系统：
        1. 创建学习目标（addGoal）
        2. 打卡记录学习（addCheckin）
        3. 查看目标列表（listGoals）
        4. 查看统计概览（getStatsOverview）
        5. 查看打卡趋势（getTrend）
        6. 查看打卡日历（getCheckinCalendar）
        7. 查看打卡记录（listCheckins）
        8. 修改目标状态（updateGoalStatus）

        规则：
        - 你可以和用户聊任何话题，不必局限于学习打卡
        - 当用户的操作涉及目标或打卡管理时，调用对应的函数
        - 如果用户想打卡但没有指定目标，先调用 listGoals 获取目标列表让用户选择
        - 日期格式统一为 yyyy-MM-dd，今天日期需要根据当前时间推断
        - 回复时使用友好、鼓励的语气
        - startDate 如果用户说"从今天开始"，使用当天日期
        - endDate 如果用户没有明确指定，可以设为 startDate 之后30天
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
     * 处理对话历史，返回 AI 回复文本
     */
    public String chat(List<Map<String, String>> history) {
        try {
            // Step 1: 调用 LLM，携带 function definitions 和完整对话历史
            JsonNode llmResponse = callLlm(history);
            String finishReason = llmResponse.path("choices").path(0).path("finish_reason").asText();

            // Step 2: 如果 LLM 要求调用函数
            if ("tool_calls".equals(finishReason)) {
                JsonNode toolCalls = llmResponse.path("choices").path(0).path("message").path("tool_calls");
                String assistantContent = llmResponse.path("choices").path(0).path("message").path("content").asText();

                // 执行函数
                JsonNode funcResult = executeToolCalls(toolCalls);

                // Step 3: 将函数执行结果返回 LLM 生成最终回复（携带完整历史）
                JsonNode finalResponse = callLlmWithToolsResult(
                        history,
                        toolCalls,
                        assistantContent,
                        funcResult
                );
                return finalResponse.path("choices").path(0).path("message").path("content").asText("操作完成！");
            }

            // Step 4: 无需函数调用，直接返回 LLM 回复
            return llmResponse.path("choices").path(0).path("message").path("content").asText("请告诉我你需要什么帮助？");

        } catch (Exception e) {
            return "抱歉，智能助手暂时无法处理你的请求，请稍后重试。你也可以通过页面菜单手动操作。";
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

        // Tools
        body.set("tools", objectMapper.readTree(TOOLS_JSON));
        body.put("tool_choice", "auto");

        String response = llmRestClient.post()
                .uri("/v1/chat/completions")
                .body(body.toString())
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(response);
    }

    private JsonNode executeToolCalls(JsonNode toolCalls) {
        ArrayNode results = objectMapper.createArrayNode();
        for (JsonNode toolCall : toolCalls) {
            String funcName = toolCall.path("function").path("name").asText();
            String callId = toolCall.path("id").asText();
            String argsStr = toolCall.path("function").path("arguments").asText();

            ObjectNode result = objectMapper.createObjectNode();
            result.put("tool_call_id", callId);
            result.put("role", "tool");

            try {
                String content = executeFunction(funcName, argsStr);
                result.put("content", content);
            } catch (Exception e) {
                result.put("content", "操作失败：" + e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    private JsonNode callLlmWithToolsResult(List<Map<String, String>> history, JsonNode toolCalls,
                                             String assistantContent, JsonNode funcResults) throws JsonProcessingException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");

        ObjectNode sysMsg = messages.addObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", SYSTEM_PROMPT);

        // 对话历史
        for (Map<String, String> msg : history) {
            ObjectNode historyMsg = messages.addObject();
            historyMsg.put("role", msg.get("role"));
            historyMsg.put("content", msg.get("content"));
        }

        // Assistant message with tool calls
        ObjectNode assistantMsg = messages.addObject();
        assistantMsg.put("role", "assistant");
        if (assistantContent != null && !assistantContent.isEmpty()) {
            assistantMsg.put("content", assistantContent);
        }
        assistantMsg.set("tool_calls", toolCalls);

        // Tool results
        for (JsonNode result : funcResults) {
            ObjectNode toolMsg = messages.addObject();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", result.path("tool_call_id").asText());
            toolMsg.put("content", result.path("content").asText());
        }

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
