package com.contentaggregation.auth.service.impl;

import com.contentaggregation.auth.entity.User;
import com.contentaggregation.auth.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of JWT token service using JJWT library.
 */
@Slf4j
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String USER_ID_CLAIM = "userId";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.issuer:content-aggregation-auth}")
    private String issuer;

    @Override
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(USER_ID_CLAIM, user.getId().toString());
        claims.put(EMAIL_CLAIM, user.getEmail());
        claims.put(ROLES_CLAIM, user.getRoles());

        return buildToken(claims, user.getEmail(), accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(USER_ID_CLAIM, user.getId().toString());

        return buildToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expiration) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return ACCESS_TOKEN_TYPE.equals(tokenType) && !isTokenExpired(token);
        } catch (Exception ex) {
            log.debug("Invalid access token: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return REFRESH_TOKEN_TYPE.equals(tokenType) && !isTokenExpired(token);
        } catch (Exception ex) {
            log.debug("Invalid refresh token: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.get(USER_ID_CLAIM, String.class);
        return UUID.fromString(userId);
    }

    @Override
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    @Override
    public LocalDateTime getExpirationFromToken(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneOffset.UTC);
    }

    @Override
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired");
            throw ex;
        } catch (UnsupportedJwtException ex) {
            log.debug("JWT token unsupported");
            throw ex;
        } catch (MalformedJwtException ex) {
            log.debug("JWT token malformed");
            throw ex;
        } catch (SignatureException ex) {
            log.debug("JWT signature invalid");
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string empty");
            throw ex;
        }
    }

    @Override
    public LocalDateTime getAccessTokenExpiration() {
        return LocalDateTime.now(ZoneOffset.UTC).plusSeconds(accessTokenExpiration / 1000);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
