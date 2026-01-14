package com.sixclassguys.maplecalendar.domain.notification.scheduler

import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val notificationService: NotificationService
) {

    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyNotification() {
        notificationService.sendEndingEventNotifications()
    }

    @Scheduled(cron = "0 * * * * *")
    fun scheduleCustomNotification() {
        notificationService.sendCustomEventNotifications()
    }
}