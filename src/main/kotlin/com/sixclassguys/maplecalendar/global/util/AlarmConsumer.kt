package com.sixclassguys.maplecalendar.global.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class AlarmConsumer(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {

    @RabbitListener(queues = ["alarm.queue"])
    fun consumeAlarm(jsonMessage: String) { // String으로 받음
        try {
            val alarmDto = objectMapper.readValue(jsonMessage, RedisAlarmDto::class.java)
            notificationService.processRedisAlarm(alarmDto)
        } catch (e: Exception) {
            // 역직렬화 실패 시 로그 처리
            println("Failed to parse alarm message: $jsonMessage")
        }
    }
}