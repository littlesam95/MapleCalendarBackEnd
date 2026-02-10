package com.sixclassguys.maplecalendar.domain.boss.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "member_boss_party_mapping")
class MemberBossPartyMapping(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "boss_party_id", nullable = false)
    val bossPartyId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "last_read_chat_id", nullable = true)
    var lastReadChatId: Long = 0L,

    @Column(name = "is_party_alarm_enabled", nullable = false)
    var isPartyAlarmEnabled: Boolean,

    @Column(name = "is_chat_alarm_enabled", nullable = false)
    var isChatAlarmEnabled: Boolean,
)