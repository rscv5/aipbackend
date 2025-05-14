package com.reports.aipbackend.controller;

import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.service.WorkOrderService;
import com.reports.aipbackend.service.UserService;
import com.reports.aipbackend.common.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/gridworker")
public class GridWorkerController {
    private static final Logger logger = LoggerFactory.getLogger(GridWorkerController.class);

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false, defaultValue = "today") String type,
            @RequestHeader("Authorization") String token) {
        try {
            logger.info("获取工单列表: type={}", type);
            Map<String, Object> claims = jwtUtils.parseToken(token.replace("Bearer ", ""));
            String openid = (String) claims.get("openid");
            Integer userId = (Integer) claims.get("userId");
            if ((openid == null || openid.isEmpty()) && userId != null) {
                User user = userService.findByUserId(userId);
                if (user != null) {
                    openid = user.getOpenid();
                }
            }
            List<WorkOrder> orders;
            if ("today".equals(type)) {
                orders = workOrderService.getTodayUnclaimedOrders();
            } else if ("my".equals(type)) {
                orders = workOrderService.getHandlerWorkOrders(openid);
            } else {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 400);
                resp.put("msg", "无效的工单类型");
                resp.put("data", null);
                return ResponseEntity.badRequest().body(resp);
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 200);
            resp.put("data", orders);
            resp.put("msg", "success");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("获取工单列表失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
            resp.put("data", null);
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    @PostMapping("/claim-order")
    public ResponseEntity<?> claimOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        try {
            Integer workId = (Integer) body.get("workId");
            logger.info("认领工单: workId={}", workId);

            Map<String, Object> claims = jwtUtils.parseToken(token.replace("Bearer ", ""));
            String openid = (String) claims.get("openid");
            Integer userId = (Integer) claims.get("userId");
            if ((openid == null || openid.isEmpty()) && userId != null) {
                User user = userService.findByUserId(userId);
                if (user != null) {
                    openid = user.getOpenid();
                }
            }

            WorkOrder workOrder = workOrderService.findById(workId);
            if (workOrder == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 404);
                resp.put("msg", "工单不存在");
                resp.put("data", null);
                return ResponseEntity.badRequest().body(resp);
            }
            if (workOrder.getHandledBy() != null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 409);
                resp.put("msg", "工单已被其他人认领");
                resp.put("data", null);
                return ResponseEntity.badRequest().body(resp);
            }
            workOrderService.claimOrder(workId, openid);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 200);
            resp.put("msg", "工单认领成功");
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("认领工单失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
            resp.put("data", null);
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}