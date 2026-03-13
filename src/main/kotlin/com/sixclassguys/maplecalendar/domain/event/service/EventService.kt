package com.sixclassguys.maplecalendar.domain.event.service

import com.sixclassguys.maplecalendar.domain.event.dto.EventResponse
import com.sixclassguys.maplecalendar.domain.event.entity.Event
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmRepository
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.infrastructure.external.NexonApiClient
import com.sixclassguys.maplecalendar.infrastructure.external.dto.EventNotice
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

@Service
class EventService(
    private val nexonApiClient: NexonApiClient,
    private val eventRepository: EventRepository,
    private val memberRepository: MemberRepository,
    private val eventAlarmRepository: EventAlarmRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun mapToResponses(events: List<Event>, userEmail: String): List<EventResponse> {
        if (events.isEmpty()) return emptyList()

        val member = memberRepository.findByEmail(userEmail) ?: return events.map { it.toDefaultResponse() }

        val isGlobalEnabled = member.isGlobalAlarmEnabled

        // 2. [핵심] 현재 조회된 이벤트 ID 리스트 추출
        val eventIds = events.map { it.id }

        // 3. [핵심] 단 한 번의 쿼리로 해당 유저의 모든 관련 알람 조회 (In-Query)
        val alarms = eventAlarmRepository.findAllActiveByMemberAndEventIds(member, eventIds)

        // 4. 빠른 조회를 위해 Map으로 변환 (EventID -> EventAlarm)
        val alarmMap = alarms.associateBy { it.event.id }

        // 5. 최종 데이터 조합
        return events.map { event ->
            val userAlarm = alarmMap[event.id]
            val finalIsRegistered = if (!isGlobalEnabled) false else (userAlarm?.isEnabled ?: false)
            EventResponse(
                id = event.id,
                title = event.title,
                url = event.url,
                thumbnailUrl = event.thumbnailUrl,
                startDate = event.startDate.toString(),
                endDate = event.endDate.toString(),
                eventTypes = event.eventTypes.map { it.name },
                isRegistered = finalIsRegistered,
                alarmTimes = if (!isGlobalEnabled) emptyList()
                else userAlarm?.alarmTimes?.filter { !it.isSent && it.alarmTime.isAfter(LocalDateTime.now()) }
                    ?.map { it.alarmTime.toString() }
                    ?.sorted() ?: emptyList(),
            )
        }
    }

    // 기본 응답 변환을 위한 확장 함수
    private fun Event.toDefaultResponse() = EventResponse(
        id = id,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        eventTypes = eventTypes.map { it.name },
        isRegistered = false,
        alarmTimes = emptyList()
    )

    fun getEventDetail(userEmail: String, eventId: Long): EventResponse? {
        val event = eventRepository.findById(eventId).orElse(null)

        return event?.let { mapToResponses(listOf(it), userEmail).firstOrNull() }
    }

    fun getTodayEvents(year: Int, month: Int, day: Int, userEmail: String): List<EventResponse> {
        val today = LocalDateTime.of(year, month, day, 0, 0)
        val events = eventRepository.getEventsForToday(today)

        return mapToResponses(events, userEmail)
    }

    fun getEventsByMonth(year: Int, month: Int, userEmail: String): List<EventResponse> {
        val startOfMonth = LocalDateTime.of(year, month, 1, 0, 0)
        val endOfMonth = startOfMonth.plusMonths(1).minusNanos(1)

        val events = eventRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            endOfMonth, startOfMonth
        )

        return mapToResponses(events, userEmail)
    }

    @Transactional
    fun refreshAndCheckEvents() {
        val dtos = nexonApiClient.getRecent20Events()

        updateEvents(dtos)

        val todayStart = LocalDate.now().atStartOfDay() // 오늘 00:00:00
        val todayEnd = LocalDate.now().atTime(LocalTime.MAX) // 오늘 23:59:59
        val closingToday = eventRepository.findAllByEndDateBetween(todayStart, todayEnd)

        if (closingToday.isNotEmpty()) {
            log.info("📢 오늘 종료되는 이벤트가 ${closingToday.size}건 있습니다!")
            closingToday.forEach {
                log.info("   - 종료 이벤트: ${it.title}")
                // TODO: 여기서 FCM 서비스 호출 (푸시 알림 발송)
            }
        } else {
            log.info("✅ 오늘 종료되는 이벤트가 없습니다.")
        }
    }

    private fun updateEvents(dtos: List<EventNotice>) {
        dtos.forEach { dto ->
            // 이벤트의 날짜를 LocalDateTime으로 파싱
            val eventStartDate = OffsetDateTime.parse(dto.dateEventStart).toLocalDateTime()
            val eventEndDate = OffsetDateTime.parse(dto.dateEventEnd).toLocalDateTime()

            val existingEvent = eventRepository.findById(dto.noticeId).orElse(null)

            if (existingEvent == null) {
                // 1. 신규 이벤트라면 추가
                val newEvent = Event(
                    id = dto.noticeId,
                    title = dto.title,
                    url = dto.url,
                    thumbnailUrl = dto.thumbnailUrl,
                    date = dto.date,
                    startDate = eventStartDate,
                    endDate = eventEndDate
                )
                eventRepository.save(newEvent)
                log.info("새로운 이벤트 등록: ${dto.title}")
            } else {
                // 2. 이미 존재한다면 변경 사항 체크 후 업데이트
                val isUpdated = existingEvent.updateIfChanged(
                    title = dto.title,
                    url = dto.url,
                    thumbnailUrl = dto.thumbnailUrl,
                    date = dto.date,
                    startDate = eventStartDate,
                    endDate = eventEndDate
                )
                if (isUpdated) {
                    log.info("이벤트 정보 수정됨: ${dto.title}")
                }
            }
        }
    }
}