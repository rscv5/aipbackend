package com.reports.aipbackend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AccessTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenUtil.class);

    @Value("${WECHAT_APPID}")
    private String appid;

    @Value("${WECHAT_SECRET}")
    private String appsecret;

    private String accessToken;
    private long accessTokenExpiresIn; // in seconds
    private long lastFetchTime; // in milliseconds

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 初始化时获取一次access_token并开始定时刷新
    public AccessTokenUtil() {
        // 初始时不立即获取，等待第一次调用 getAccessToken() 时获取
        // 确保 @Value 注入完成后再使用 appid 和 appsecret
        // 在实际应用中，可以通过 @PostConstruct 或 CommandLineRunner 来在应用启动时初始化
    }

    public String getAccessToken() {
        // 检查token是否过期或即将过期 (提前10分钟刷新)
        if (accessToken == null || System.currentTimeMillis() > (lastFetchTime + (accessTokenExpiresIn * 1000) - 600000)) {
            fetchAccessToken();
        }
        return accessToken;
    }

    private synchronized void fetchAccessToken() {
        logger.info("开始获取新的access_token...");
        String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appid, appsecret);

        try {
            String response = restTemplate.getForObject(tokenUrl, String.class);
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (result.containsKey("access_token")) {
                this.accessToken = (String) result.get("access_token");
                this.accessTokenExpiresIn = ((Number) result.get("expires_in")).longValue();
                this.lastFetchTime = System.currentTimeMillis();
                logger.info("成功获取access_token，有效期 {} 秒", accessTokenExpiresIn);

                // 启动定时任务，在过期前刷新 (提前10分钟)
                scheduler.schedule(this::fetchAccessToken, accessTokenExpiresIn - 600, TimeUnit.SECONDS); // 提前10分钟刷新
            } else {
                logger.error("获取access_token失败: {}", result.get("errmsg"));
            }
        } catch (IOException e) {
            logger.error("获取access_token请求失败", e);
        } catch (Exception e) {
            logger.error("解析access_token响应失败", e);
        }
    }
} 