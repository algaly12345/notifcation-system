package com.notify.producer.controller;

import com.notify.producer.model.NotificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final String TOPIC = "notifications";
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    /**
     * Send notification to all users (broadcast)
     * POST /api/notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("📤 Sending notification: {} | Type: {} | ID: {}",
                request.getTitle(), request.getType(), request.getNotificationId());

        CompletableFuture<SendResult<String, NotificationRequest>> future =
                kafkaTemplate.send(TOPIC, request.getNotificationId(), request);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Notification sent successfully to partition: {} | offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send notification: {}", ex.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "notificationId", request.getNotificationId(),
                "topic", TOPIC,
                "message", "Notification queued successfully"
        ));
    }

    /**
     * Send notification to specific user
     * POST /api/notifications/send/user/{userId}
     */
    @PostMapping("/send/user/{userId}")
    public ResponseEntity<Map<String, Object>> sendToUser(
            @PathVariable String userId,
            @Valid @RequestBody NotificationRequest request) {

        request.setUserId(userId);
        log.info("📤 Sending notification to user: {} | Title: {}", userId, request.getTitle());

        kafkaTemplate.send(TOPIC, request.getNotificationId(), request);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "notificationId", request.getNotificationId(),
                "targetUser", userId,
                "message", "Notification sent to user: " + userId
        ));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "status", "UP",
                "kafka", "connected"
        ));
    }
}
