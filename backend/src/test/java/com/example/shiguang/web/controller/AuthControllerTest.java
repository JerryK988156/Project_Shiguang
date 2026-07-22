package com.example.shiguang.web.controller;

import com.example.shiguang.common.utls.PasswordUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(properties = {
    "spring.cache.type=simple",
    "DB_VERIFY_ON_STARTUP=false",
    "DB_AUTO_INIT_ON_STARTUP=false"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSuccess() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "zhangsan", "password", "123456"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.username").value("zhangsan"));
    }

    @Test
    void loginWrongPassword() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "zhangsan", "password", "wrongpass"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void registerSuccess() throws Exception {
        String uniqueUser = "ut" + (System.currentTimeMillis() % 100000000);
        String body = objectMapper.writeValueAsString(Map.of(
                "username", uniqueUser,
                "password", "test123456",
                "nickname", "TestUser"
        ));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.username").value(uniqueUser));
    }

    @Test
    void getUserInfoWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/userInfo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordEncodeAndMatch() {
        String rawPassword = "123456";
        String encoded = PasswordUtils.encode(rawPassword);
        assert encoded != null && encoded.startsWith("$2");
        assert PasswordUtils.matches(rawPassword, encoded);
    }
}
