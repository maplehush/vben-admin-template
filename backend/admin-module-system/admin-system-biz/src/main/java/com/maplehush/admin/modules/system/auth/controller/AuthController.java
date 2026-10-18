package com.maplehush.admin.modules.system.auth.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.config.RefreshCookieSupport;
import com.maplehush.admin.modules.system.auth.dto.LoginDTO;
import com.maplehush.admin.modules.system.auth.service.ICaptchaService;
import com.maplehush.admin.modules.system.auth.service.ILoginService;
import com.maplehush.admin.modules.system.auth.service.ISessionService;
import com.maplehush.admin.modules.system.auth.vo.CaptchaChallengeVO;
import com.maplehush.admin.modules.system.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ICaptchaService captchas;
    private final ILoginService logins;
    private final ISessionService sessions;
    private final RefreshCookieSupport cookies;

    public AuthController(
            ICaptchaService captchas, ILoginService logins, ISessionService sessions, RefreshCookieSupport cookies) {
        this.captchas = captchas;
        this.logins = logins;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @GetMapping("/captcha")
    public ApiResult<CaptchaChallengeVO> captcha(HttpServletRequest request) {
        return ApiResult.ok(captchas.issue(request.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(
            @Valid @RequestBody LoginDTO body, HttpServletRequest request, HttpServletResponse response) {
        ISessionService.TokenPair tokens = logins.login(
                body.username(),
                body.password(),
                body.captchaId(),
                body.captchaCode(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        cookies.set(response, tokens.refreshToken());
        return ApiResult.ok(new LoginVO(tokens.accessToken()));
    }

    @PostMapping(value = "/refresh", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        cookies.requireAllowedOrigin(request);
        ISessionService.TokenPair tokens = sessions.refresh(refreshToken);
        cookies.set(response, tokens.refreshToken());
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(tokens.accessToken());
    }

    @PostMapping("/logout")
    public ApiResult<String> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        cookies.requireAllowedOrigin(request);
        sessions.revoke(refreshToken);
        cookies.clear(response);
        return ApiResult.ok("");
    }
}
