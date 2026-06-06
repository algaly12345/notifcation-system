# 🔔 نظام الإشعارات الفوري — Real-Time Notification System
## Spring Boot + Apache Kafka + Redis + WebSocket

---

## 🏗️ هيكل المشروع

```
notification-system/
├── docker-compose.yml           ← Kafka + Zookeeper + Redis + UI Monitors
├── client-app/
│   └── index.html               ← واجهة العميل (تفتح في المتصفح)
├── notification-service/        ← PORT 8081 — يستقبل الإشعارات ويرسلها لـ Kafka
│   ├── pom.xml
│   └── src/main/java/com/notify/producer/
│       ├── NotificationServiceApplication.java
│       ├── controller/NotificationController.java
│       ├── model/NotificationRequest.java
│       └── config/KafkaProducerConfig.java
└── consumer-service/            ← PORT 8082 — يستمع Kafka + يخزن Redis + يبث WebSocket
    ├── pom.xml
    └── src/main/java/com/notify/consumer/
        ├── ConsumerServiceApplication.java
        ├── consumer/NotificationConsumer.java
        ├── service/NotificationRedisService.java
        ├── websocket/NotificationWebSocketController.java
        ├── model/Notification.java
        └── config/
            ├── KafkaConsumerConfig.java
            ├── RedisConfig.java
            └── WebSocketConfig.java
```

---

## 🚀 خطوات التشغيل

### 1. تشغيل Infrastructure (Kafka + Redis)
```bash
cd notification-system
docker-compose up -d
```

انتظر 30 ثانية حتى يرتفع Kafka، ثم تحقق:
```bash
docker-compose ps
```

### 2. تشغيل notification-service (PORT 8081)
```bash
cd notification-service
./mvnw spring-boot:run
```

### 3. تشغيل consumer-service (PORT 8082)
```bash
cd consumer-service
./mvnw spring-boot:run
```

### 4. فتح واجهة العميل
```bash
# افتح الملف في المتصفح
open client-app/index.html
```

---

## 📮 طلبات Postman

### أولاً — إرسال إشعار عام (Broadcast)
```
POST http://localhost:8081/api/notifications/send
Content-Type: application/json

{
    "title": "تحديث النظام",
    "message": "تم تحديث النظام بنجاح إلى الإصدار 2.0",
    "type": "SUCCESS"
}
```

### ثانياً — إشعار معلومة
```
POST http://localhost:8081/api/notifications/send
Content-Type: application/json

{
    "title": "اجتماع قادم",
    "message": "لديك اجتماع بعد 30 دقيقة في قاعة A",
    "type": "INFO"
}
```

### ثالثاً — إشعار تحذير
```
POST http://localhost:8081/api/notifications/send
Content-Type: application/json

{
    "title": "تحذير: مساحة التخزين",
    "message": "مساحة القرص وصلت إلى 85%، يُنصح بالتنظيف",
    "type": "WARNING"
}
```

### رابعاً — إشعار خطأ
```
POST http://localhost:8081/api/notifications/send
Content-Type: application/json

{
    "title": "خطأ في الاتصال",
    "message": "فشل الاتصال بقاعدة البيانات، يرجى المراجعة",
    "type": "ERROR"
}
```

### خامساً — إرسال لمستخدم محدد
```
POST http://localhost:8081/api/notifications/send/user/user123
Content-Type: application/json

{
    "title": "رسالة شخصية",
    "message": "لديك طلب جديد يحتاج موافقتك",
    "type": "INFO"
}
```

### إشعار نظام
```
POST http://localhost:8081/api/notifications/send
Content-Type: application/json

{
    "title": "صيانة مجدولة",
    "message": "سيتم إيقاف النظام للصيانة في منتصف الليل",
    "type": "SYSTEM"
}
```

---

## 🔍 طلبات الاستعلام

### جلب كل الإشعارات من Redis
```
GET http://localhost:8082/api/notifications
```

### جلب إشعارات مستخدم
```
GET http://localhost:8082/api/notifications/user/user123
```

### عدد الإشعارات غير المقروءة
```
GET http://localhost:8082/api/notifications/user/user123/unread
```

### مسح إشعارات مستخدم
```
DELETE http://localhost:8082/api/notifications/user/user123
```

### Health Check - notification-service
```
GET http://localhost:8081/api/notifications/health
```

### Health Check - consumer-service
```
GET http://localhost:8082/api/notifications/health
```

---

## 🎛️ لوحات المراقبة

| الأداة | الرابط | الوصف |
|--------|--------|--------|
| Kafka UI | http://localhost:8090 | مراقبة الـ Topics والرسائل |
| Redis Commander | http://localhost:8091 | مراقبة البيانات في Redis |
| Client App | client-app/index.html | واجهة الإشعارات |

---

## 🔄 تدفق البيانات

```
Postman
   │
   ▼ POST /api/notifications/send
notification-service (8081)
   │
   ▼ Publish to topic: "notifications"
Apache Kafka
   │
   ▼ @KafkaListener
consumer-service (8082)
   │
   ├──► Redis (يخزن الإشعار لـ 7 أيام)
   │
   └──► WebSocket /topic/notifications
          │
          ▼
     client-app/index.html
     (يظهر الإشعار فورياً)
```

---

## 🛠️ المتطلبات

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

---

## 📝 أنواع الإشعارات

| النوع | الرمز | الوصف |
|-------|-------|-------|
| INFO | 🔵 | معلومة عامة |
| SUCCESS | 🟢 | عملية ناجحة |
| WARNING | 🟡 | تحذير يحتاج انتباه |
| ERROR | 🔴 | خطأ أو فشل |
| SYSTEM | 🟣 | رسائل النظام |
