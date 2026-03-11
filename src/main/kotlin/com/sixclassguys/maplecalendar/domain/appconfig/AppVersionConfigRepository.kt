package com.sixclassguys.maplecalendar.domain.appconfig

import com.sixclassguys.maplecalendar.domain.appconfig.entity.AppVersionConfig
import com.sixclassguys.maplecalendar.domain.notification.dto.Platform
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppVersionConfigRepository : JpaRepository<AppVersionConfig, Long> {

    // 💡 플랫폼(ANDROID, IOS)에 해당하는 설정을 찾는 메소드
    fun findByPlatform(platform: Platform): AppVersionConfig?
}