package com.sixclassguys.maplecalendar.domain.event.service

import com.sixclassguys.maplecalendar.domain.event.entity.Event
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
import java.time.format.DateTimeFormatter

@Service
class EventService(
    private val nexonApiClient: NexonApiClient,
    private val eventRepository: EventRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getTodayEvents(year: Int, month: Int, day: Int): List<Event> {
        val today = LocalDateTime.of(year, month, day, 0, 0)

        return eventRepository.getEventsForToday(today)
    }

    fun getEventsByMonth(year: Int, month: Int): List<Event> {
        val startOfMonth = LocalDateTime.of(year, month, 1, 0, 0)
        val endOfMonth = startOfMonth.plusMonths(1).minusNanos(1)

        return eventRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            endOfMonth,
            startOfMonth
        )
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