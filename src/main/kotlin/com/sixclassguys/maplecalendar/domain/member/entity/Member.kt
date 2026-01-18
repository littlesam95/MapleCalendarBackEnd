package com.sixclassguys.maplecalendar.domain.member.entity

import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    var nexonApiKey: String, // 유저의 API Key (넥슨 서버 통신용)

    @Column(name = "representative_ocid")
    var representativeOcid: String? = null, // 대표 캐릭터 식별자

    // 💡 정규 알림(오늘 종료 이벤트 등) 수신 여부 추가
    @Column(name = "is_global_alarm_enabled", nullable = false)
    var isGlobalAlarmEnabled: Boolean = true,

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL])
    val tokens: MutableList<NotificationToken> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val eventAlarms: MutableList<EventAlarm> = mutableListOf()
)