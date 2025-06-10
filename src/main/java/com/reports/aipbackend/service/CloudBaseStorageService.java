package com.reports.aipbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reports.aipbackend.util.AccessTokenUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudBaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudBaseStorageService.class);

    @Autowired
    private AccessTokenUtil accessTokenUtil;

    @Value("${wechat.env}")
    private String env;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取文件上传链接
     * @param fileName 文件名
     * @param filePathPrefix 文件在云存储中的路径前缀（如 'workorder/'）
     * @return 包含上传链接和 COS 凭证的 Map
     */
    public Map<String, Object> getUploadFileLink(String fileName, String filePathPrefix) throws IOException {
        String accessToken = accessTokenUtil.getAccessToken();
        if (accessToken == null) {
            logger.error("获取access_token失败，无法获取文件上传链接");
            return null;
        }

        String filePathUrl = String.format("https://api.weixin.qq.com/tcb/uploadfile?access_token=%s", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("env", env);
        requestData.put("path", filePathPrefix + fileName);

        String requestBodyJson = objectMapper.writeValueAsString(requestData);

        org.springframework.http.HttpEntity<String> requestEntity = new org.springframework.http.HttpEntity<>(requestBodyJson, headers);

        logger.info("请求微信云托管获取文件上传链接: {}", filePathUrl);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(filePathUrl, requestEntity, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            String responseBodyString = responseEntity.getBody();
            logger.info("微信云托管API原始响应: {}", responseBodyString);
            try {
                Map<String, Object> responseBody = objectMapper.readValue(responseBodyString, Map.class);
                if (responseBody.containsKey("url")) {
                    logger.info("成功获取文件上传链接: {}", responseBody.get("url"));
                    return responseBody;
                } else {
                    logger.error("获取文件上传链接失败: {}", responseBody.get("errmsg"));
                    return null;
                }
            } catch (IOException e) {
                logger.error("解析微信云托管API响应失败，响应内容非JSON格式: {}", responseBodyString, e);
                return null;
            }
        } else {
            logger.error("请求微信云托管获取文件上传链接失败: 状态码={}, 原始响应={}", responseEntity.getStatusCode(), responseEntity.getBody());
            return null;
        }
    }

    /**
     * 执行文件上传到 COS
     * @param uploadLinkInfo 包含上传 URL 和 COS 凭证的 Map (由 getUploadFileLink 返回)
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 上传是否成功 (HTTP 204 No Content 代表成功)
     */
    public boolean uploadFileToCos(Map<String, Object> uploadLinkInfo, byte[] fileBytes, String fileName, String filePathPrefix) {
        String fileUploadUrl = (String) uploadLinkInfo.get("url");
        String authorization = (String) uploadLinkInfo.get("authorization");
        String securityToken = (String) uploadLinkInfo.get("token");
        String cosFileId = (String) uploadLinkInfo.get("cos_file_id");

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFilePost = new HttpPost(fileUploadUrl);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("key", filePathPrefix + fileName);
        builder.addTextBody("Signature", authorization);
        builder.addTextBody("x-cos-security-token", securityToken);
        builder.addTextBody("x-cos-meta-fileid", cosFileId);
        builder.addPart("file", new ByteArrayBody(fileBytes, ContentType.DEFAULT_BINARY, fileName));

        HttpEntity multipart = builder.build();
        uploadFilePost.setEntity(multipart);

        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadFilePost)) {
            int statusCode = uploadResponse.getStatusLine().getStatusCode();
            logger.info("文件上传到COS响应状态码: {}", statusCode);
            return statusCode == 204; // 204 No Content 表示成功
        } catch (IOException e) {
            logger.error("文件上传到COS失败", e);
            return false;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("关闭HttpClient失败", e);
            }
        }
    }
} 