package com.maplehush.admin.modules.system.common.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** DTO/VO 拆分后保持用户列表的字段契约；时间格式由启动模块的全局 Jackson 配置验证。 */
class ResponseContractTest {
    @Test
    void userPageKeepsFieldNames() {
        UserSummaryVO user = new UserSummaryVO(
                "2",
                "maple",
                "测试工程师",
                null,
                null,
                null,
                "1",
                "研发部",
                "ENABLED",
                LocalDateTime.of(2026, 10, 1, 20, 26, 32),
                false);

        String json = new ObjectMapper().writeValueAsString(ApiResult.ok(new PageResult<>(List.of(user), 1)));

        assertTrue(json.contains("\"code\":0"));
        assertTrue(json.contains("\"items\""));
        assertTrue(json.contains("\"total\":1"));
        assertTrue(json.contains("\"username\":\"maple\""));
        assertTrue(json.contains("\"primaryDeptName\":\"研发部\""));
        assertTrue(json.contains("\"createTime\""));
    }
}
