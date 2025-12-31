package com.sixclassguys.maplecalendar.domain.notification.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.sixclassguys.maplecalendar.domain.notification.dto.TokenRequest
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import com.sixclassguys.maplecalendar.domain.notification.repository.NotificationTokenRepository
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class NotificationService(
    private val notificationTokenRepository: NotificationTokenRepository,
    private val eventRepository: EventRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun registerToken(request: TokenRequest) {
        val existingToken = notificationTokenRepository.findByToken(request.token)

        if (existingToken != null) {
            existingToken.platform = request.platform
            existingToken.lastRegisteredAt = LocalDateTime.now()
            // JPA의 Dirty Checking으로 인해, 별도의 save 호출 없이도 업데이트가 가능하다.
        } else {
            notificationTokenRepository.save(
                NotificationToken(
                    token = request.token,
                    platform = request.platform
                )
            )
        }
    }

    fun sendEndingEventNotifications() {
        // 1. 오늘 종료되는 이벤트 조회
        val startOfToday = LocalDate.now().atStartOfDay()
        val endOfToday = LocalDate.now().atTime(LocalTime.MAX)
        val endingEvents = eventRepository.findAllByEndDateBetween(startOfToday, endOfToday)

        // 2. 메시지 내용 구성 (이벤트 유무에 따라 다르게)
        val title = "메이플 캘린더 오늘의 소식 🍁"
        val randomMessages = listOf(
            "오늘도 즐거운 메이플 되세요!",
            "재획하기 좋은 날씨네요!",
            "스타포스 대박 나시길 기원합니다.",
            "일퀘 몬파 하러갑시다!"
        )
        val body = if (endingEvents.isNotEmpty()) {
            "오늘 종료되는 이벤트가 ${endingEvents.size}개 있습니다! 늦기 전에 확인하세요."
        } else { randomMessages.random() }

        // 3. 모든 토큰 조회
        val tokens = notificationTokenRepository.findAll()
        if (tokens.isEmpty()) {
            log.info("등록된 FCM 토큰이 없어 알림을 보내지 않았습니다.")
            return
        }

        // 4. 발송 로직
        tokens.forEach { tokenEntity ->
            val message = Message.builder()
                .setToken(tokenEntity.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .setAndroidConfig(
                    AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build())
                .build()

            try {
                FirebaseMessaging.getInstance().send(message)
                log.info("푸시 알림 발송 성공: ${tokenEntity.token.take(8)}...")
            } catch (e: Exception) {
                log.error("푸시 알림 발송 실패: ${e.message}")
            }
        }
    }
}