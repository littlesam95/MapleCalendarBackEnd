package com.sixclassguys.maplecalendar.domain.member.entity

import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import com.sixclassguys.maplecalendar.global.config.ApiKeyConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
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

    @Column(unique = true, nullable = false, length = 1000)
    @Convert(converter = ApiKeyConverter::class) // 암호화 적용
    var nexonApiKey: String, // 유저의 API Key (넥슨 서버 통신용)

    // 검색용 필드: 평문을 SHA-256 등으로 해싱한 값을 저장 (인덱스 가능)
    @Column(name = "api_key_hash", unique = true, nullable = false, length = 512)
    var apiKeyHash: String = "",

    @Column(name = "representative_ocid")
    var representativeOcid: String? = null, // 대표 캐릭터 식별자

    @Column(name = "char_action")
    var charAction: String = "A00.0",

    @Column(name = "char_emotion")
    var charEmotion: String = "E00.0",

    @Column(name = "char_weapon_motion")
    var charWeaponMotion: String = "W00",

    // 💡 정규 알림(오늘 종료 이벤트 등) 수신 여부 추가
    @Column(name = "is_global_alarm_enabled", nullable = false)
    var isGlobalAlarmEnabled: Boolean = true,

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL])
    val tokens: MutableList<NotificationToken> = mutableListOf(),

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val eventAlarms: MutableList<EventAlarm> = mutableListOf()
)