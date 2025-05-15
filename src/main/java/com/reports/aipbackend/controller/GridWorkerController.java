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

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        try {
            Integer workId = (Integer) body.get("workId");
            String handledDesc = (String) body.get("handledDesc");
            List<String> handledImages = (List<String>) body.get("handledImages");
            logger.info("提交反馈: workId={}, handledDesc={}, handledImages={}", workId, handledDesc, handledImages);

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
            if (!openid.equals(workOrder.getHandledBy())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 403);
                resp.put("msg", "只有认领该工单的网格员才能提交反馈");
                resp.put("data", null);
                return ResponseEntity.badRequest().body(resp);
            }
            workOrderService.submitWorkOrderFeedback(workId, openid, handledDesc, handledImages);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 200);
            resp.put("msg", "反馈提交成功");
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("提交反馈失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
            resp.put("data", null);
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    @PostMapping("/report-to-captain")
    public ResponseEntity<?> reportToCaptain(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String token) {
        try {
            Integer workId = (Integer) body.get("workId");
            logger.info("上报片区长: workId={}", workId);

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
            if (!openid.equals(workOrder.getHandledBy())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 403);
                resp.put("msg", "只有认领该工单的网格员才能上报片区长");
                resp.put("data", null);
                return ResponseEntity.badRequest().body(resp);
            }
            workOrderService.updateStatus(workId, "已上报", openid, null, null);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 200);
            resp.put("msg", "上报片区长成功");
            resp.put("data", null);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("上报片区长失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
            resp.put("data", null);
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}