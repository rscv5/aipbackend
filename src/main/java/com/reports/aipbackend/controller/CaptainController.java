package com.reports.aipbackend.controller;

import com.reports.aipbackend.common.JwtUtils;
import com.reports.aipbackend.entity.WorkOrder;
import com.reports.aipbackend.service.WorkOrderService;
import com.reports.aipbackend.service.UserService;
import com.reports.aipbackend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.HashMap;

@RestController
@RequestMapping("/api/captain")
public class CaptainController {

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    /**
     * 获取片区长管理的工单列表
     * @param type 工单类型：all-全部，today-今日提交，processing-处理中，reported-已上报，completed-处理完
     * @return 工单列表
     */
    @GetMapping("/workorders")
    public ResponseEntity<?> getWorkOrders(@RequestParam(defaultValue = "all") String type) {
        try {
            // 从JWT中获取用户ID
            Integer userId = jwtUtils.getUserIdFromToken();
            List<WorkOrder> orders = workOrderService.getCaptainWorkOrders(userId, type);
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", orders,
                "message", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "获取工单列表失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 重新分配工单
     * @param request 包含工单ID、网格员ID和截止时间
     * @return 操作结果
     */
    @PostMapping("/reassign")
    public ResponseEntity<?> reassignWorkOrder(@RequestBody Map<String, Object> request) {
        try {
            Integer workId = (Integer) request.get("workId");
            String gridWorkerOpenid = (String) request.get("gridWorkerOpenid");
            String deadlineStr = (String) request.get("deadline");
            LocalDate deadline = null;
            if (deadlineStr != null && !deadlineStr.isEmpty()) {
                // 兼容 yyyy-MM-dd 和 yyyy-MM-ddTHH:mm:ss 两种格式
                if (deadlineStr.length() == 10) {
                    deadline = LocalDate.parse(deadlineStr);
                } else if (deadlineStr.length() > 10) {
                    deadline = LocalDate.parse(deadlineStr.substring(0, 10));
                } else {
                    throw new IllegalArgumentException("截止时间格式错误");
                }
            }
            if (workId == null || gridWorkerOpenid == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "工单ID和网格员ID不能为空"
                ));
            }

            // 获取当前片区长的openid
            String captainOpenid = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                // 假设 UserDetails 的getUsername()返回的是用户名，需要根据用户名查询用户获取openid
                // 或者你的 UserDetails 实现直接包含了 openid
                // 这里假设 UserDetails 的 username 就是 openid
                User currentUser = userService.findByUsername(userDetails.getUsername());
                if (currentUser != null) {
                    captainOpenid = currentUser.getOpenid();
                }
            }

            if (captainOpenid == null) {
                 return ResponseEntity.status(401).body(Map.of(
                    "code", 401,
                    "message", "无法获取片区长信息或未登录"
                 ));
            }

            workOrderService.reassignWorkOrder(workId, gridWorkerOpenid, deadlineStr, captainOpenid);
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "重新分配成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "重新分配失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 获取所有网格员列表
     */
    @GetMapping("/grid-workers")
    public ResponseEntity<?> getGridWorkers() {
        try {
            List<Map<String, Object>> workers = userService.getUsersByRole("网格员").stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("openid", user.getOpenid());
                    map.put("username", user.getUsername());
                    return map;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", workers,
                "message", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "获取网格员失败：" + e.getMessage()
            ));
        }
    }
} 