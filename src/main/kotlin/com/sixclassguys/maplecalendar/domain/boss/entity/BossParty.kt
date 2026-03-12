package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.boss.enums.BossDifficulty
import com.sixclassguys.maplecalendar.domain.boss.enums.BossType
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
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

    @OneToMany(mappedBy = "bossParty", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val members: List<BossPartyMember> = mutableListOf(),

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
) {

    // 승인된(ACCEPTED) 멤버들만 필터링해서 가져오는 프로퍼티
    val acceptedMembers: List<BossPartyMember>
        get() = members.filter { it.joinStatus == JoinStatus.ACCEPTED }

    // 현재 보스와 난이도에 맞는 최대 인원수 반환
    val maxCapacity: Int
        get() = boss.getMaxPartyMemberCount(difficulty)

    // 파티가 꽉 찼는지 여부
    fun isFull(): Boolean = currentMemberCount >= maxCapacity

    // 편의를 위해 현재 참여 인원수도 쉽게 가져오게 함
    val currentMemberCount: Int
        get() = acceptedMembers.size
}