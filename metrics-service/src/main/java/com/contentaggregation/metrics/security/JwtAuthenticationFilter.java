package com.contentaggregation.metrics.security;

import com.contentaggregation.metrics.client.AuthServiceClient;
import com.contentaggregation.metrics.client.dto.UserDto;
import com.contentaggregation.metrics.exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthServiceClient authServiceClient;

    public JwtAuthenticationFilter(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);
            if (StringUtils.hasText(jwt)) {
                UserDto user = authServiceClient.validateToken(jwt);
                if (user != null) {
                    UserPrincipal principal = new UserPrincipal(user);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.error("Unexpected auth error: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.startsWith("/v3/api-docs");
    }
}

