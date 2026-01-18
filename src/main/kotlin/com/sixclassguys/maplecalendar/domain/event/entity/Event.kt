package com.sixclassguys.maplecalendar.domain.event.entity

import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(
    @Id
    val id: Long, // notice_id를 PK로 사용
    var title: String,
    var url: String,
    var thumbnailUrl: String?,
    var date: String,
    var startDate: LocalDateTime,
    var endDate: LocalDateTime,

    // 💡 설정된 알람 시간들 (별도 테이블로 관리됨)
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val registeredAlarms: MutableList<EventAlarm> = mutableListOf()
) {

    fun updateIfChanged(
        title: String,
        url: String,
        thumbnailUrl: String?,
        date: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Boolean {
        if (this.title == title &&
            this.url == url &&
            this.thumbnailUrl == thumbnailUrl &&
            this.date == date &&
            this.startDate == startDate &&
            this.endDate == endDate
        ) return false

        this.title = title
        this.url = url
        this.thumbnailUrl = thumbnailUrl
        this.date = date
        this.startDate = startDate
        this.endDate = endDate

        return true
    }
}