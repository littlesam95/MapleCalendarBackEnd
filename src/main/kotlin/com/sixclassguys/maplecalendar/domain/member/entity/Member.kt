package com.sixclassguys.maplecalendar.domain.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
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

    @Column(name = "fcm_token")
    var fcmToken: String? = null // 푸시 알림용 토큰
)