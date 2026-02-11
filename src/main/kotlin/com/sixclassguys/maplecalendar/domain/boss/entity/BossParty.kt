package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.boss.enums.BossDifficulty
import com.sixclassguys.maplecalendar.domain.boss.enums.BossType
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction


@Entity
@Table(name = "boss_party")
@SQLDelete(sql = "UPDATE boss_party SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
class BossParty(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 1000)
    var title: String,

    @Column(nullable = false, length = 1000)
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "boss", nullable = false)
    var boss: BossType,

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    var difficulty: BossDifficulty,

    @Column(name = "alarm_day_of_week")
    var alarmDayOfWeek: DayOfWeek? = null,

    @Column(name = "alarm_hour")
    var alarmHour: Int? = null,

    @Column(name = "alarm_minute")
    var alarmMinute: Int? = null,

    @Column(name = "alarm_message", length = 100)
    var alarmMessage: String? = null,

    @OneToMany(mappedBy = "bossParty", fetch = FetchType.LAZY)
    val members: List<BossPartyMember> = emptyList(),

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
)