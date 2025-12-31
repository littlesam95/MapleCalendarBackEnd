package com.sixclassguys.maplecalendar.domain.notification.scheduler

import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notificationService: NotificationService
) {
    // 매일 00시에 실행
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyNotification() {
        notificationService.sendEndingEventNotifications()
    }

    // 테스트용: 1분마다 실행 (확인 후 주석 처리하세요)
    // @Scheduled(fixedRate = 60000)
    // fun test() = notificationService.sendEndingEventNotifications()
}