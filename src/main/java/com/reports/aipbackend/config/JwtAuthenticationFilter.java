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
    private final List<String> excludedPaths = Arrays.asList(
            "/login/",
        "/api/grid/login",
        "/api/user/login",
        "/api/area/login",
            "/api/generate-password"
    );
    
    public JwtAuthenticationFilter(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludedPaths.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            logger.info("=== JWT过滤器处理请求 ===");
            logger.info("请求路径: {}", request.getRequestURI());
            logger.info("请求方法: {}", request.getMethod());
            
            String jwt = getJwtFromRequest(request);
            logger.info("获取到的JWT: {}", jwt != null ? jwt : "未找到或格式错误");

            if (jwt != null) {
                try {
                    // 校验token是否过期
                    if (jwtUtils.isTokenExpired(jwt)) {
                        logger.warn("JWT已过期");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                        response.getWriter().write("Token expired");
                        return;
                    }

                    // 验证token签名
                    if (jwtUtils.validateToken(jwt)) {
                        logger.info("JWT验证成功");
                        var claims = jwtUtils.getClaimsFromToken(jwt);
                        logger.info("从JWT中解析出的Claims: {}", claims);
                        
                        // 尝试通过 userId 加载用户
                        Integer userId = claims.get("userId", Integer.class);
                        if (userId != null) {
                            UserDetails userDetails = userDetailsService.loadUserById(userId);
                            if (userDetails != null) {
                                logger.info("通过 userId 加载到的UserDetails: username={}, authorities={}", userDetails.getUsername(), userDetails.getAuthorities());
                                setAuthentication(userDetails, request);
                                logger.info("认证信息已设置到SecurityContext");
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                        
                        // 如果 userId 不存在或加载失败，尝试使用 username (通常是 subject)
                        String username = claims.getSubject(); // getSubject() 返回的是 username
                        if (username != null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (userDetails != null) {
                                logger.info("通过 username 加载到的UserDetails: username={}, authorities={}", userDetails.getUsername(), userDetails.getAuthorities());
                                setAuthentication(userDetails, request);
                                logger.info("认证信息已设置到SecurityContext");
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                        
                        logger.warn("无法从JWT中获取有效的用户信息或加载UserDetails失败");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
                        response.getWriter().write("Invalid user information in token");
                        return;
                    } else {
                        logger.warn("JWT签名验证失败");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                        response.getWriter().write("Token validation failed");
                        return;
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException eje) {
                    logger.warn("JWT处理异常: Token已过期", eje.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token expired");
                    return;
                } catch (io.jsonwebtoken.SignatureException se) {
                    logger.warn("JWT处理异常: 签名验证失败", se.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token signature");
                    return;
                } catch (Exception e) {
                    logger.error("JWT处理过程中发生未知错误", e);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Token processing error");
                    return;
                }
            } else {
                logger.info("请求中未找到JWT，继续过滤器链");
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("过滤器处理过程中发生未知错误", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal server error");
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("认证信息已设置到SecurityContext，用户: {}, authorities: {}", 
            userDetails.getUsername(), 
            userDetails.getAuthorities());
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