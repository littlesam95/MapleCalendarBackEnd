package com.sixclassguys.maplecalendar.global.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.global.config.RabbitConfig
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class AlarmProducer(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun reserveAlarm(alarmDto: RedisAlarmDto, sendAt: LocalDateTime) {
        val delay = Duration.between(LocalDateTime.now(), sendAt).toMillis()
        log.info("🚀 알람 예약 시도: targetId=${alarmDto.targetId}, 지연시간=${delay}ms")
        if (delay < 0) return // 이미 지난 시간은 발송 제외

        // 객체를 JSON 문자열로 직접 변환 (Converter 의존성 제거)
        val jsonMessage = objectMapper.writeValueAsString(alarmDto)

        val routingKey = when (alarmDto.type) {
            AlarmType.EVENT -> RabbitConfig.EVENT_ROUTING_KEY
            AlarmType.BOSS -> RabbitConfig.BOSS_ROUTING_KEY
            else -> RabbitConfig.BOSS_ROUTING_KEY
        }

        rabbitTemplate.convertAndSend(RabbitConfig.DELAYED_EXCHANGE, routingKey, jsonMessage) { message ->
            message.messageProperties.setHeader("x-delay", delay)
            message
        }
    }
}