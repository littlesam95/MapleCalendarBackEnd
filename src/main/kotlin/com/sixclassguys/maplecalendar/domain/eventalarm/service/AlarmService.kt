package com.sixclassguys.maplecalendar.domain.eventalarm.service

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.eventalarm.dto.AlarmRequest
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarmTime
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmRepository
import com.sixclassguys.maplecalendar.domain.member.service.MemberService
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AlarmService(
    private val memberService: MemberService,
    private val eventRepository: EventRepository,
    private val eventAlarmRepository: EventAlarmRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveOrUpdateAlarm(apiKey: String, request: AlarmRequest): EventResponse { // 💡 반환 타입 변경
        val member = memberService.getMemberByRawKey(apiKey)
        val event = eventRepository.findById(request.eventId)
            .orElseThrow { throw Exception("Event not found") }

        val alarm = eventAlarmRepository.findByMemberAndEvent(member, event)
            ?: EventAlarm(member = member, event = event)

        alarm.isEnabled = request.isEnabled

        // 💡 해결책 1: 리스트를 비우고 다시 채우는 대신 새로운 리스트 객체를 생성해서 할당해 보세요.
        // 만약 엔티티에서 alarmTimes가 val이라면 아래처럼 MutableList 조작 후
        // 확실하게 save()를 호출해야 합니다.
        val requestedTimes = request.alarmTimes.map { LocalDateTime.parse(it) }
        alarm.alarmTimes.removeIf { !it.isSent }

        // 💡 새로 들어온 시간들을 엔티티 객체로 변환하여 추가
        requestedTimes.forEach { time ->
            // 중복 추가 방지 (이미 존재하는 시간인지 체크)
            if (alarm.alarmTimes.none { it.alarmTime == time }) {
                alarm.alarmTimes.add(
                    EventAlarmTime(
                        eventAlarm = alarm, // 부모 참조 필수
                        alarmTime = time,
                        isSent = false
                    )
                )
            }
        }

        val savedAlarm = eventAlarmRepository.saveAndFlush(alarm) // 💡 명시적으로 DB에 반영 (Flush)

        log.info("Saved result count: ${savedAlarm.alarmTimes.size}")

        // 💡 저장된 엔티티들을 조합해 EventResponse로 변환하여 반환
        return EventResponse(
            id = event.id,
            title = event.title,
            url = event.url,
            thumbnailUrl = event.thumbnailUrl,
            startDate = event.startDate.toString(),
            endDate = event.endDate.toString(),
            isRegistered = savedAlarm.isEnabled,
            alarmTimes = savedAlarm.alarmTimes.filter { !it.isSent }
                .map { it.alarmTime.toString() }
                .sorted()
        )
    }

    @Transactional
    fun toggleAlarmStatus(apiKey: String, eventId: Long): EventResponse {
        val member = memberService.getMemberByRawKey(apiKey)
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
            isRegistered = savedAlarm.isEnabled,
            alarmTimes = savedAlarm.alarmTimes.filter { !it.isSent }
                .map { it.alarmTime.toString() }
                .sorted()
        )
    }
}