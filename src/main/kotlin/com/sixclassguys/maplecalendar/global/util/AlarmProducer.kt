package com.sixclassguys.maplecalendar.global.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class AlarmProducer(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper // 스프링 기본 제공 빈 사용
) {

    fun reserveAlarm(alarmDto: RedisAlarmDto, sendAt: LocalDateTime) {
        val delay = Duration.between(LocalDateTime.now(), sendAt).toMillis()
        if (delay < 0) return

        // 💡 핵심: 객체를 직접 던지지 않고 JSON 문자열로 변환
        val jsonMessage = objectMapper.writeValueAsString(alarmDto)

        rabbitTemplate.convertAndSend("alarm.exchange", "alarm.routing.key", jsonMessage) { message ->
            message.messageProperties.setHeader("x-delay", delay)
            message
        }
    }
}