package com.maplehush.admin.common.api.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ApiResultTest {
    @Test
    void failureKeepsTheExistingEnvelope() {
        ApiResult<Void> result = ApiResult.fail(40901, "数据冲突或仍被引用");
        assertEquals(40901, result.code());
        assertNull(result.data());
        assertEquals("数据冲突或仍被引用", result.message());
    }
}
