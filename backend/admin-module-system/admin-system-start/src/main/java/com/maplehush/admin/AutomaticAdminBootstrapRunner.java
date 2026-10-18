package com.maplehush.admin;

import com.maplehush.admin.modules.system.auth.service.IBootstrapAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 仅在显式开启时，为全新数据库创建初始管理员 */
@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.auto-enabled", havingValue = "true")
public class AutomaticAdminBootstrapRunner implements ApplicationRunner {

    private final IBootstrapAdminService service;
    private final String username;

    public AutomaticAdminBootstrapRunner(
            IBootstrapAdminService service, @Value("${app.bootstrap-admin.username:admin}") String username) {
        this.service = service;
        this.username = username;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long userId = service.createFirstAdminIfEmpty(username);
        if (userId != null) {
            System.out.printf("Initial administrator created: username=%s, userId=%d%n", username, userId);
        }
    }
}
