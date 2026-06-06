package com.notify.consumer.service;

import com.notify.consumer.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisService {

    private final RedisTemplate<String, Notification> redisTemplate;

    private static final String GLOBAL_KEY = "notifications:global";
    private static final String USER_KEY_PREFIX = "notifications:user:";
    private static final long TTL_DAYS = 7;
    private static final long MAX_NOTIFICATIONS = 100;

    /**
     * Save notification to Redis
     */
    public void save(Notification notification) {
        String key = notification.getUserId() != null
                ? USER_KEY_PREFIX + notification.getUserId()
                : GLOBAL_KEY;

        // Push to list (newest first)
        redisTemplate.opsForList().leftPush(key, notification);

        // Keep only last 100 notifications
        redisTemplate.opsForList().trim(key, 0, MAX_NOTIFICATIONS - 1);

        // Set TTL
        redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));

        log.info("💾 Saved to Redis | key: {} | id: {}", key, notification.getNotificationId());
    }

    /**
     * Get all global notifications
     */
    public List<Notification> getGlobalNotifications() {
        return getNotifications(GLOBAL_KEY);
    }

    /**
     * Get notifications for specific user (global + user-specific)
     */
    public List<Notification> getUserNotifications(String userId) {
        List<Notification> all = new ArrayList<>();
        all.addAll(getNotifications(GLOBAL_KEY));
        all.addAll(getNotifications(USER_KEY_PREFIX + userId));
        // Sort by timestamp desc
        all.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        return all;
    }

    /**
     * Get unread count
     */
    public long getUnreadCount(String userId) {
        List<Notification> notifications = getUserNotifications(userId);
        return notifications.stream().filter(n -> !n.isRead()).count();
    }

    /**
     * Clear all notifications for a user
     */
    public void clearUserNotifications(String userId) {
        redisTemplate.delete(USER_KEY_PREFIX + userId);
        log.info("🗑️ Cleared notifications for user: {}", userId);
    }

    private List<Notification> getNotifications(String key) {
        List<Notification> result = redisTemplate.opsForList().range(key, 0, -1);
        return result != null ? result : new ArrayList<>();
    }
}
