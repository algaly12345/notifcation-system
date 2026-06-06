package com.notify.consumer.consumer;

import com.notify.consumer.model.Notification;
import com.notify.consumer.service.NotificationRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRedisService redisService;

    @KafkaListener(
            topics = "notifications",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload Notification notification,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📥 Received | partition:{} | offset:{} | id:{}", partition, offset, notification.getNotificationId());

        // 1. Save to Redis
        redisService.save(notification);

        if (notification.getUserId() != null && !notification.getUserId().isBlank()) {
            // إشعار خاص — يُرسل على topic خاص بالمستخدم
            String userTopic = "/topic/notifications/" + notification.getUserId();
            log.info("📡 Sending to personal topic: {}", userTopic);
            messagingTemplate.convertAndSend(userTopic, notification);
        } else {
            // إشعار عام لكل المتصلين
            log.info("📡 Broadcasting to all");
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        }
    }
}
