package com.sixclassguys.maplecalendar.domain.boss.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "boss_party_alarm_time")
class BossPartyAlarmTime(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "boss_party_member_mapping_id", nullable = false)
    val bossPartyMemberMappingId: Long,

    @Column(nullable = false)
    val alarmTime: LocalDateTime,

    @Column(nullable = false, length = 500)
    val message: String,

    @Column(nullable = false)
    var isSent: Boolean = false
)
