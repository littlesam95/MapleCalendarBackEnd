package com.sixclassguys.maplecalendar.domain.member.entity

import com.sixclassguys.maplecalendar.domain.auth.entity.RefreshToken
import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
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
import java.time.LocalDateTime

@Entity
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "provider", nullable = false)
    var provider: String,

    @Column(name = "provider_id", nullable = false)
    var providerId: String, // Firebase UID

    @Column(name = "email", unique = true, nullable = false)
    var email: String,

    @Column(name = "nickname")
    var nickname: String? = null,

    @Column(name = "profileImageUrl")
    var profileImageUrl: String? = null,

    @Column(name = "is_global_alarm_enabled", nullable = false)
    var isGlobalAlarmEnabled: Boolean = false,

    @Column(name = "representative_ocid")
    var representativeOcid: String? = null,

    @Column(name = "last_login_at", nullable = false)
    var lastLoginAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "background_image_url", length = 500)
    var backgroundImageUrl: String? = null,

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL])
    val tokens: MutableList<NotificationToken> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val eventAlarms: MutableList<EventAlarm> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val characters: MutableList<MapleCharacter> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val nexonApiKeys: MutableList<NexonApiKey> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf()
)