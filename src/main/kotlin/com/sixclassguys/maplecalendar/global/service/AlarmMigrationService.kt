package com.sixclassguys.maplecalendar.global.service

import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmTimeRepository
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AlarmMigrationService(
    private val eventAlarmTimeRepository: EventAlarmTimeRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional(readOnly = true)
    fun migrateAlarmsToRabbitMQ() {
        log.info("🚚 [Migration] Postgres -> RabbitMQ 알람 예약 이사 시작")

        // 1. 이벤트 알람 마이그레이션 (기존 유지)
        val pendingEventTimes = eventAlarmTimeRepository.findAllByIsSentFalseWithDetails()
        pendingEventTimes.filter { it.alarmTime.isAfter(LocalDateTime.now()) }
            .forEach { time ->
                alarmProducer.reserveAlarm(
                    RedisAlarmDto(
                        type = AlarmType.EVENT,
                        targetId = time.id,
                        memberId = time.eventAlarm.member.id,
                        title = "⏰ 이벤트 알림",
                        message = "[${time.eventAlarm.event.title}] 설정하신 알람 시간입니다!",
                        partyId = 0L,
                    ), time.alarmTime
                )
            }

        // 2. [수정] 보스 파티 알람 마이그레이션
        val pendingBossTimes = bossPartyAlarmTimeRepository.findByIsSentFalse()
        var bossCount = 0

        pendingBossTimes.filter { it.alarmTime.isAfter(LocalDateTime.now()) }
            .forEach { time ->
                val dto = RedisAlarmDto(
                    type = AlarmType.BOSS,
                    targetId = time.id,
                    partyId = time.bossPartyId, // 수정된 필드 사용
                    memberId = 0L,              // 파티 단위 발송이므로 0 처리
                    title = "⚔️ 보스 파티 알림",
                    message = time.message
                )
                alarmProducer.reserveAlarm(dto, time.alarmTime)
                bossCount++
            }

        log.info("✅ [Migration] 보스 알람 $bossCount 건 재등록 완료")
    }
}