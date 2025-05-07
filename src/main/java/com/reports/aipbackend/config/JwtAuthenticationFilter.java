package com.reports.aipbackend.config;

import com.reports.aipbackend.common.JwtUtils;
import com.reports.aipbackend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
/**
 * JWT认证过滤器
 * 用于处理JWT token的验证和用户认证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    
    // 定义不需要验证的路径
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/api/grid/login",
        "/api/user/login",
        "/api/area/login"
    );
    
    public JwtAuthenticationFilter(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            logger.info("=== 开始处理请求 ===");
            logger.info("请求路径: {}", request.getRequestURI());
            logger.info("请求方法: {}", request.getMethod());
            
            String jwt = getJwtFromRequest(request);
            logger.info("获取到的JWT: {}", jwt);

            if (jwt != null) {
                try {
                    if (jwtUtils.validateToken(jwt)) {
                        logger.info("JWT验证成功");
                        var claims = jwtUtils.getClaimsFromToken(jwt);
                        
                        // 尝试通过 userId 加载用户
                        Integer userId = claims.get("userId", Integer.class);
                        if (userId != null) {
                            UserDetails userDetails = userDetailsService.loadUserById(userId);
                            if (userDetails != null) {
                                setAuthentication(userDetails, request);
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                        
                        // 如果 userId 不存在或加载失败，尝试使用 username
                        String username = claims.get("username", String.class);
                        if (username != null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (userDetails != null) {
                                setAuthentication(userDetails, request);
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                        
                        logger.warn("无法从JWT中获取有效的用户信息");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("Invalid user information in token");
                        return;
                    } else {
                        logger.warn("JWT验证失败");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("Token validation failed");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("JWT处理过程中发生错误", e);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Token processing error");
                    return;
                }
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("过滤器处理过程中发生错误", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal server error");
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("认证信息已设置到SecurityContext");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.info("Authorization header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 