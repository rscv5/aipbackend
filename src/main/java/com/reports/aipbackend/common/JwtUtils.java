package com.reports.aipbackend.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.reports.aipbackend.entity.User;

import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 获取 SecretKey
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成token
    public String generateToken(String username, String role, Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        return doGenerateToken(claims, username);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 解析所有claims
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 获取指定claim
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 获取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 获取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 检查token是否过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 验证token
    public Boolean validateToken(String token) {
        logger.info("开始验证token");
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.error("token验证失败", e);
            return false;
        }
    }

    // 解析token为Map
    public Map<String, Object> parseToken(String token) {
        logger.info("开始解析token claims");
        try {
            Claims claims = getAllClaimsFromToken(token);
            Map<String, Object> result = new HashMap<>();
            result.put("username", claims.getSubject());
            result.put("role", claims.get("role"));
            result.put("userId", claims.get("userId"));
            result.put("iat", claims.getIssuedAt());
            result.put("exp", claims.getExpiration());
            logger.info("token claims解析成功: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("token claims解析失败", e);
            throw e;
        }
    }

    // 直接获取Claims
    public Claims getClaimsFromToken(String token) {
        logger.info("开始获取token claims");
        try {
            Claims claims = getAllClaimsFromToken(token);
            logger.info("token claims获取成功");
            return claims;
        } catch (Exception e) {
            logger.error("token claims获取失败", e);
            throw e;
        }
    }

    // 生成token
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("role", user.getRole());
        claims.put("isSuperAdmin", user.getIsSuperAdmin());
        claims.put("sub", user.getUsername());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24小时有效期
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // ====== 你需要添加的内容 ======
    /**
     * 从请求中获取token
     * @return token字符串
     */
    private String getTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("无法获取当前请求");
        }
        HttpServletRequest request = attributes.getRequest();
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("未找到认证信息");
    }

    /**
     * 从JWT中获取用户ID
     * @return 用户ID
     */
    public Integer getUserIdFromToken() {
        String token = getTokenFromRequest();
        Map<String, Object> claims = parseToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return (Integer) userIdObj;
        } else if (userIdObj instanceof Number) {
            return ((Number) userIdObj).intValue();
        } else if (userIdObj != null) {
            return Integer.parseInt(userIdObj.toString());
        }
        throw new RuntimeException("token中未包含userId");
    }
}