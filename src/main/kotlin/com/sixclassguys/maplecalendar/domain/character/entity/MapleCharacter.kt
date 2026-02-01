package com.sixclassguys.maplecalendar.domain.character.entity

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "maple_characters")
class MapleCharacter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(nullable = false, length = 200)
    val ocid: String,

    @Column(name = "character_name", nullable = false, length = 20)
    var characterName: String,

    @Column(name = "world_name", nullable = false, length = 20)
    var worldName: String,

    @Column(name = "character_gender", nullable = false, length = 10)
    var characterGender: String,

    @Column(name = "character_class", nullable = false, length = 20)
    var characterClass: String,

    @Column(name = "character_class_level", nullable = false, length = 20)
    var characterClassLevel: String,

    @Column(name = "character_level", nullable = false)
    var characterLevel: Long,

    @Column(name = "character_exp", nullable = false)
    var characterExp: Long,

    @Column(name = "character_exp_rate", nullable = false, length = 10)
    var characterExpRate: String,

    @Column(name = "character_guild_name", length = 20)
    var characterGuildName: String? = null,

    @Column(name = "character_image", length = 2000)
    var characterImage: String? = null,

    @Column(name = "character_date_create", nullable = false)
    val characterDateCreate: LocalDate,

    @Column(name = "access_flag", nullable = false, length = 10)
    var accessFlag: String,

    @Column(name = "liberation_quest_clear", length = 10)
    var liberationQuestClear: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true, // 현재 유효한 캐릭터인지 여부

    @Column(name = "last_updated_at", nullable = false)
    var lastUpdatedAt: LocalDateTime = LocalDateTime.now()
)