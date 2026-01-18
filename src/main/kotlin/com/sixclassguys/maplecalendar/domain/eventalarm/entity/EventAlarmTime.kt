package com.sixclassguys.maplecalendar.domain.eventalarm.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "event_alarm_times")
class EventAlarmTime(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_alarm_id")
    val eventAlarm: EventAlarm,

    @Column(name = "alarm_time", nullable = false)
    val alarmTime: LocalDateTime,

    @Column(name = "is_sent", nullable = false)
    var isSent: Boolean = false // 💡 알림 전송 완료 여부 컬럼 추가
)