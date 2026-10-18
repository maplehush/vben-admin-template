package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.modules.system.auth.config.AuthProperties;
import com.maplehush.admin.modules.system.auth.service.IAccessTokenService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Service;

/** 签发并验证短期访问令牌；有效期使用绝对时间，不随数据库展示时区变化。 */
@Service
public class AccessTokenServiceImpl implements IAccessTokenService {

    private final AuthProperties properties;
    private final Clock clock;
    private final MACSigner signer;
    private final MACVerifier verifier;

    public AccessTokenServiceImpl(AuthProperties properties, Clock clock) throws JOSEException {
        this.properties = properties;
        this.clock = clock;
        this.signer = new MACSigner(Base64.getDecoder().decode(properties.jwtSecretBase64()));
        this.verifier = new MACVerifier(Base64.getDecoder().decode(properties.jwtSecretBase64()));
    }

    @Override
    public TokenIdentity verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(verifier)
                    || !properties.issuer().equals(claims.getIssuer())
                    || claims.getExpirationTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(clock.instant())
                    || claims.getIssueTime() == null
                    || claims.getIssueTime().toInstant().isAfter(clock.instant())
                    || claims.getSubject() == null) {
                throw ApiException.invalidAccessToken();
            }
            long userId = Long.parseLong(claims.getSubject());
            Object sessionClaim = claims.getClaim("sid");
            long sessionId = Long.parseLong(String.valueOf(sessionClaim));
            if (userId < 1 || sessionId < 1) {
                throw ApiException.invalidAccessToken();
            }
            return new TokenIdentity(userId, sessionId);
        } catch (ParseException | JOSEException | NumberFormatException exception) {
            throw ApiException.invalidAccessToken();
        }
    }

    @Override
    public String issue(long userId, long sessionId) {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(Long.toString(userId))
                .claim("sid", sessionId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(properties.accessTtl())))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign access token", exception);
        }
    }
}
