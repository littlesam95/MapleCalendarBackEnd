package com.sixclassguys.maplecalendar.domain.eventalarm.service

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.eventalarm.dto.AlarmRequest
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarmTime
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmRepository
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class AlarmService(
    private val memberRepository: MemberRepository,
    private val eventRepository: EventRepository,
    private val eventAlarmRepository: EventAlarmRepository,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveOrUpdateAlarm(userEmail: String, request: AlarmRequest): EventResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val event = eventRepository.findById(request.eventId)
            .orElseThrow { Exception("Event not found") }

        val alarm = eventAlarmRepository.findByMemberAndEvent(member, event)
            ?: EventAlarm(member = member, event = event)

        alarm.isEnabled = request.isEnabled

        val requestedTimes = request.alarmTimes.map { LocalDateTime.parse(it) }
        alarm.alarmTimes.removeIf { !it.isSent }

        // 1. 새 시간 리스트 저장 (이 시점에는 아직 ID가 0임)
        val addedTimes = requestedTimes.filter { time ->
            alarm.alarmTimes.none { it.alarmTime == time }
        }.map { time ->
            EventAlarmTime(eventAlarm = alarm, alarmTime = time, isSent = false)
        }

        alarm.alarmTimes.addAll(addedTimes)

        // 2. DB 반영 (이때 모든 addedTimes 객체들에 ID가 할당됨)
        val savedAlarm = eventAlarmRepository.saveAndFlush(alarm)

        // 3. [수정] 할당된 ID로 예약 수행
        // savedAlarm.alarmTimes에서 방금 추가했던(addedTimes와 시간이 일치하는) 녀석들을 찾습니다.
        savedAlarm.alarmTimes.filter { it.alarmTime in requestedTimes && !it.isSent }.forEach { newTime ->
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), event.endDate.toLocalDate())
            val dDayText = when {
                daysLeft > 0L -> "${daysLeft}일 남았어요."
                daysLeft == 0L -> "오늘 종료됩니다! 서두르세요!"
                else -> "이벤트가 종료되었어요."
            }

            val dto = RedisAlarmDto(
                type = AlarmType.EVENT,
                targetId = newTime.id, // 💡 이제 무조건 DB 할당 ID가 들어감
                memberId = member.id,
                partyId = 0L,
                title = "⏰ 설정하신 이벤트 알림 시간이에요!",
                message = "[${event.title}] $dDayText"
            )
            alarmProducer.reserveAlarm(dto, newTime.alarmTime)
        }

        log.info("Saved and Reserved result count: ${savedAlarm.alarmTimes.size}")

        return EventResponse(
            id = event.id,
            title = event.title,
            url = event.url,
            thumbnailUrl = event.thumbnailUrl,
            startDate = event.startDate.toString(),
            endDate = event.endDate.toString(),
            eventTypes = event.eventTypes.map { it.name },
            isRegistered = savedAlarm.isEnabled,
            alarmTimes = savedAlarm.alarmTimes.filter { !it.isSent }
                .map { it.alarmTime.toString() }
                .sorted()
        )
    }

    @Transactional
    fun toggleAlarmStatus(userEmail: String, eventId: Long): EventResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val event = eventRepository.findById(eventId).orElseThrow { throw Exception("Event not found") }

        // 1. 기존 알람 설정을 찾거나, 없으면 새로 생성
        val alarm = eventAlarmRepository.findByMemberAndEvent(member, event)
            ?: EventAlarm(member = member, event = event, isEnabled = false)

        // 2. 상태 반전 (OFF -> ON / ON -> OFF)
        alarm.isEnabled = !alarm.isEnabled

        val savedAlarm = eventAlarmRepository.save(alarm)

        // 3. 변경된 결과 반환 (이전 서비스에서 만든 변환 로직 활용)
        return EventResponse(
            id = event.id,
            title = event.title,
            url = event.url,
            thumbnailUrl = event.thumbnailUrl,
            startDate = event.startDate.toString(),
            endDate = event.endDate.toString(),
            event.eventTypes.map { it.name },
            isRegistered = savedAlarm.isEnabled,
            alarmTimes = savedAlarm.alarmTimes.filter { !it.isSent }
                .map { it.alarmTime.toString() }
                .sorted()
        )
    }
}