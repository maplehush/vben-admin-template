package com.maplehush.admin.modules.system.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 确保每个控制器路由都有明确的认证入口策略 */
class RouteSecurityPolicyTest {
    @Test
    void everyControllerRouteHasExactlyOnePolicy() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<RouteKey> endpoints = new HashSet<>();
        for (var bean : scanner.findCandidateComponents("com.maplehush.admin.modules.system")) {
            Class<?> controller = Class.forName(bean.getBeanClassName());
            RequestMapping prefix = controller.getAnnotation(RequestMapping.class);
            String base = prefix == null ? "" : prefix.value()[0];
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    endpoints.add(new RouteKey(
                            HttpMethod.GET,
                            base + method.getAnnotation(GetMapping.class).value()[0]));
                }
                if (method.isAnnotationPresent(PostMapping.class)) {
                    endpoints.add(new RouteKey(
                            HttpMethod.POST,
                            base + method.getAnnotation(PostMapping.class).value()[0]));
                }
                if (method.isAnnotationPresent(PutMapping.class)) {
                    endpoints.add(new RouteKey(
                            HttpMethod.PUT,
                            base + method.getAnnotation(PutMapping.class).value()[0]));
                }
                if (method.isAnnotationPresent(DeleteMapping.class)) {
                    endpoints.add(new RouteKey(
                            HttpMethod.DELETE,
                            base + method.getAnnotation(DeleteMapping.class).value()[0]));
                }
            }
        }
        var policies = RouteSecurityPolicy.ROUTES.stream()
                .map(route -> new RouteKey(route.method(), route.path()))
                .toList();
        assertEquals(policies.size(), new HashSet<>(policies).size(), "路由策略不能重复");
        assertEquals(endpoints, new HashSet<>(policies), "控制器路由必须全部列入认证入口策略");
        assertEquals(
                Set.of(
                        new RouteKey(HttpMethod.GET, "/auth/captcha"),
                        new RouteKey(HttpMethod.POST, "/auth/login"),
                        new RouteKey(HttpMethod.POST, "/auth/refresh"),
                        new RouteKey(HttpMethod.POST, "/auth/logout")),
                RouteSecurityPolicy.ROUTES.stream()
                        .filter(route -> route.access() == RouteSecurityPolicy.Access.PUBLIC)
                        .map(route -> new RouteKey(route.method(), route.path()))
                        .collect(java.util.stream.Collectors.toSet()),
                "匿名路由仅限认证入口");
    }

    private record RouteKey(HttpMethod method, String path) {}
}
