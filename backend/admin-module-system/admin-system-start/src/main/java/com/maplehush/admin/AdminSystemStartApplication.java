package com.maplehush.admin;

import com.maplehush.admin.common.util.DateUtils;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AdminSystemStartApplication {

    @Bean
    public Clock applicationClock() {
        return Clock.system(DateUtils.ZONE);
    }

    public static void main(String[] args) {
        SpringApplication.run(AdminSystemStartApplication.class, args);
    }
}
