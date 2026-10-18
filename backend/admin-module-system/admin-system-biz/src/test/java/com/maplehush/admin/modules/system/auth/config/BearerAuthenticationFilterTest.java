package com.maplehush.admin.modules.system.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maplehush.admin.modules.system.auth.service.ICurrentUserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BearerAuthenticationFilterTest {

    @Test
    void accessCodesIsProtectedByFirstPasswordChangeGate() throws Exception {
        ICurrentUserService users = mock(ICurrentUserService.class);
        when(users.authenticate("valid-token")).thenReturn(new ICurrentUserService.CurrentUser(1, 9, true));
        var filter = new BearerAuthenticationFilter(users);
        var request = new MockHttpServletRequest("GET", "/auth/codes");
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(users).authenticate("valid-token");
        assertEquals(403, response.getStatus());
    }
}
