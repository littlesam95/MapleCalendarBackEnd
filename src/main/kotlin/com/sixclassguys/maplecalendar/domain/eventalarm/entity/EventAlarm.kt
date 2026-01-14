package com.sixclassguys.maplecalendar.domain.eventalarm.entity

import com.sixclassguys.maplecalendar.domain.event.entity.Event
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "event_alarms")
class EventAlarm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    val event: Event,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,

    // 💡 사용자가 설정한 여러 개의 알람 시간을 저장하는 테이블 분리
    @OneToMany(mappedBy = "eventAlarm", cascade = [CascadeType.ALL], orphanRemoval = true)
    var alarmTimes: MutableList<EventAlarmTime> = mutableListOf()
)