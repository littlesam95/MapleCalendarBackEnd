package com.sixclassguys.maplecalendar.infrastructure.persistence.event

import com.sixclassguys.maplecalendar.domain.event.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventRepository : JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.startDate <= :today AND e.endDate >= :today")
    fun getEventsForToday(today: LocalDateTime): List<Event>

    // endDate가 파라미터로 넘어온 날짜와 일치하는 이벤트 리스트 조회
    @Query("SELECT e FROM Event e WHERE e.endDate >= :start AND e.endDate <= :end")
    fun findAllByEndDateBetween(start: LocalDateTime, end: LocalDateTime): List<Event>

    fun findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
        endOfMonth: LocalDateTime,
        startOfMonth: LocalDateTime
    ): List<Event>
}