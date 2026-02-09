package com.sixclassguys.maplecalendar.global.service

import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.MemberBossPartyMappingRepository
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmTimeRepository
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AlarmMigrationService(
    private val eventAlarmTimeRepository: EventAlarmTimeRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val memberBossPartyMappingRepository: MemberBossPartyMappingRepository,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 애플리케이션 시작 시 실행되어 Postgres에 저장된 미발송 알람을 RabbitMQ로 옮깁니다.
     */
    @EventListener(ApplicationReadyEvent::class)
    @Transactional(readOnly = true)
    fun migrateAlarmsToRabbitMQ() {
        log.info("🚚 [Migration] Postgres -> RabbitMQ 알람 예약 이사 시작")

        // 1. 이벤트 알람 마이그레이션 (EventAlarm)
        val pendingEventTimes = eventAlarmTimeRepository.findAllByIsSentFalseWithDetails()
        var eventCount = 0
        pendingEventTimes.forEach { time ->
            // 이미 지난 과거 시간의 알람은 예약하지 않음 (선택 사항)
            if (time.alarmTime.isAfter(LocalDateTime.now())) {
                val dto = RedisAlarmDto(
                    type = AlarmType.EVENT,
                    targetId = time.id!!,
                    memberId = time.eventAlarm.member.id,
                    title = "⏰ 이벤트 알림",
                    message = "[${time.eventAlarm.event.title}] 설정하신 알람 시간입니다!"
                )
                alarmProducer.reserveAlarm(dto, time.alarmTime)
                eventCount++
            }
        }

        // 2. 보스 파티 알람 마이그레이션 (BossPartyAlarmTime)
        // 보스 알람은 mappingId를 통해 유저를 찾아야 함
        val pendingBossTimes = bossPartyAlarmTimeRepository.findByIsSentFalse()
        var bossCount = 0
        pendingBossTimes.forEach { time ->
            if (time.alarmTime.isAfter(LocalDateTime.now())) {
                // 매핑 테이블에서 memberId 조회
                val mapping = memberBossPartyMappingRepository.findByIdOrNull(time.bossPartyMemberMappingId)

                mapping?.let {
                    val dto = RedisAlarmDto(
                        type = AlarmType.BOSS,
                        targetId = time.id,
                        memberId = it.memberId,
                        title = "⚔️ 보스 파티 알림",
                        message = time.message
                    )
                    alarmProducer.reserveAlarm(dto, time.alarmTime)
                    bossCount++
                }
            }
        }

        log.info("✅ [Migration] 완료: 이벤트($eventCount 건), 보스($bossCount 건) 예약되었습니다.")
    }
}