package com.example.shiguang.service;

import com.example.shiguang.common.utls.SessionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.cache.type=simple",
    "DB_VERIFY_ON_STARTUP=false",
    "DB_AUTO_INIT_ON_STARTUP=false"
})
class GoalServiceTest {

    @Autowired
    private GoalService goalService;

    @BeforeEach
    void setUp() {
        // Mock a logged-in user (zhangsan, id=3) for service tests
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SessionUtils.LOGIN_USER_ID, 3L);
        request.setAttribute(SessionUtils.LOGIN_USER_ROLE, "user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testContextLoads() {
        assertNotNull(goalService);
    }

    @Test
    void listGoalsForUser() {
        var goals = goalService.list(null);
        assertNotNull(goals);
        assertTrue(goals.size() > 0);
    }

    @Test
    void getGoalDetail() {
        // Goal id=1 should exist for zhangsan (user id=3)
        var goal = goalService.detail(1L);
        assertNotNull(goal);
        assertEquals("每天背 50 个单词", goal.getTitle());
    }
}
