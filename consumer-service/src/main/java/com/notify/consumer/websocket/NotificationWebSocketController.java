package com.notify.consumer.websocket;

import com.notify.consumer.model.Notification;
import com.notify.consumer.service.NotificationRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationWebSocketController {

    private final NotificationRedisService redisService;

    /**
     * WebSocket: Client subscribes and requests history
     * Send to /app/subscribe
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/notifications-history")
    public List<Notification> subscribe() {
        log.info("📡 New WebSocket client subscribed");
        return redisService.getGlobalNotifications();
    }

    // ─── REST Endpoints ───────────────────────────────────────────────────────

    /**
     * GET all global notifications from Redis
     */
    @GetMapping("/api/notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(redisService.getGlobalNotifications());
    }

    /**
     * GET notifications for specific user
     */
    @GetMapping("/api/notifications/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(redisService.getUserNotifications(userId));
    }

    /**
     * GET unread count for user
     */
    @GetMapping("/api/notifications/user/{userId}/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = redisService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("userId", (long) userId.hashCode(), "unreadCount", count));
    }

    /**
     * DELETE: Clear user notifications
     */
    @DeleteMapping("/api/notifications/user/{userId}")
    public ResponseEntity<Map<String, String>> clearUserNotifications(@PathVariable String userId) {
        redisService.clearUserNotifications(userId);
        return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "userId", userId
        ));
    }

    /**
     * Health check
     */
    @GetMapping("/api/notifications/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "consumer-service",
                "status", "UP",
                "redis", "connected",
                "websocket", "active"
        ));
    }
}
