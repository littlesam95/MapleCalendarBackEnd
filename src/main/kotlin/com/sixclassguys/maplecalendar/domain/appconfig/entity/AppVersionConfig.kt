package com.sixclassguys.maplecalendar.domain.appconfig.entity

import com.sixclassguys.maplecalendar.domain.notification.dto.Platform
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "app_version_config")
class AppVersionConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    val platform: Platform, // ANDROID, IOS

    val latestVersionCode: Int,    // 최신 버전 (예: 40)
    val minVersionCode: Int,       // 최소 지원 버전 (예: 35)
    val latestVersionName: String, // 표시용 이름 (예: "1.0.4")

    val updateMessage: String,     // 팝업 메시지
    val storeUrl: String           // 스토어 주소
)