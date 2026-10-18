package com.maplehush.admin;

import com.maplehush.admin.modules.system.auth.service.IBootstrapAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private final IBootstrapAdminService service;
    private final ApplicationContext context;
    private final String username;

    public AdminBootstrapRunner(
            IBootstrapAdminService service,
            ApplicationContext context,
            @Value("${app.bootstrap-admin.username:}") String username) {
        this.service = service;
        this.context = context;
        this.username = username;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (context instanceof WebApplicationContext) {
            throw new IllegalStateException(
                    "Administrator bootstrap must run with --spring.main.web-application-type=none");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Set --app.bootstrap-admin.username=<username>");
        }
        long userId = service.createFirstAdmin(username);
        System.out.printf("Initial administrator created: username=%s, userId=%d%n", username, userId);
    }
}
