package com.maplehush.admin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maplehush.admin.modules.system.audit.vo.LoginLogVO;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import tools.jackson.databind.ObjectMapper;

class JacksonConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(JacksonConfiguration.class)
            .withPropertyValues("spring.jackson.time-zone=GMT+8", "spring.jackson.date-format=yyyy-MM-dd HH:mm:ss");

    @Test
    void unannotatedJavaTimeFieldsUseGlobalApiFormat() {
        context.run(application -> {
            ObjectMapper json = application.getBean(ObjectMapper.class);
            String value = json.writeValueAsString(new TimeView(
                    LocalDateTime.of(2026, 10, 4, 14, 30, 12), LocalDate.of(2026, 10, 4), LocalTime.of(14, 30, 12)));

            assertEquals(
                    "{\"createdAt\":\"2026-10-04 14:30:12\",\"day\":\"2026-10-04\",\"clockTime\":\"14:30:12\"}", value);
            assertEquals(
                    LocalDateTime.of(2026, 10, 4, 14, 30, 12),
                    json.readValue("\"2026-10-04 14:30:12\"", LocalDateTime.class));
        });
    }

    @Test
    void httpJsonConverterUsesSameGlobalTimeFormat() {
        context.run(application -> {
            JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(
                    application.getBean(tools.jackson.databind.json.JsonMapper.class));
            MockHttpOutputMessage output = new MockHttpOutputMessage();
            converter.write(
                    new TimeView(
                            LocalDateTime.of(2026, 10, 4, 14, 30, 12),
                            LocalDate.of(2026, 10, 4),
                            LocalTime.of(14, 30, 12)),
                    MediaType.APPLICATION_JSON,
                    output);
            assertEquals(
                    "{\"createdAt\":\"2026-10-04 14:30:12\",\"day\":\"2026-10-04\",\"clockTime\":\"14:30:12\"}",
                    output.getBodyAsString());
        });
    }

    @Test
    void userAndAuditResponsesUseGlobalFormatWithoutFieldAnnotations() {
        context.run(application -> {
            ObjectMapper json = application.getBean(ObjectMapper.class);
            LocalDateTime time = LocalDateTime.of(2026, 10, 4, 14, 30, 12);
            UserSummaryVO user =
                    new UserSummaryVO("2", "maple", "测试工程师", null, null, null, "1", "研发部", "ENABLED", time, false);
            LoginLogVO log = new LoginLogVO("1", "maple", true, null, "127.0.0.1", "test", time);

            assertTrue(json.writeValueAsString(user).contains("\"createTime\":\"2026-10-04 14:30:12\""));
            assertTrue(json.writeValueAsString(log).contains("\"occurTime\":\"2026-10-04 14:30:12\""));
        });
    }

    @Test
    void fieldFormatOverridesGlobalDefault() {
        context.run(application -> {
            ObjectMapper json = application.getBean(ObjectMapper.class);
            assertEquals(
                    "{\"occurredAt\":\"2026/10/04 14:30\"}",
                    json.writeValueAsString(new SpecialTimeView(LocalDateTime.of(2026, 10, 4, 14, 30, 12))));
        });
    }

    @Test
    void legacyDateStillUsesConfiguredBeijingZone() {
        context.run(application -> {
            ObjectMapper json = application.getBean(ObjectMapper.class);
            assertEquals(
                    "{\"createdAt\":\"2026-10-04 14:30:12\"}",
                    json.writeValueAsString(
                            new LegacyTimeView(Date.from(java.time.Instant.parse("2026-10-04T06:30:12Z")))));
        });
    }

    private record TimeView(LocalDateTime createdAt, LocalDate day, LocalTime clockTime) {}

    private record SpecialTimeView(
            @JsonFormat(pattern = "yyyy/MM/dd HH:mm") LocalDateTime occurredAt) {}

    private record LegacyTimeView(Date createdAt) {}
}
