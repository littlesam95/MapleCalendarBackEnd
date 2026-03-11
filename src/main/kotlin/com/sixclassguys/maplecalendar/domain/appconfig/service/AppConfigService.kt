package com.sixclassguys.maplecalendar.domain.appconfig.service

import com.sixclassguys.maplecalendar.domain.appconfig.AppVersionConfigRepository
import com.sixclassguys.maplecalendar.domain.appconfig.dto.AppVersionResponse
import com.sixclassguys.maplecalendar.domain.notification.dto.Platform
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppConfigService(
    private val appVersionConfigRepository: AppVersionConfigRepository
) {

    @Transactional(readOnly = true)
    fun getUpdateInfo(platform: Platform, currentVersionCode: Int): AppVersionResponse {
        val config = appVersionConfigRepository.findByPlatform(platform)
            ?: throw Exception("해당 플랫폼의 설정이 없습니다.")

        return AppVersionResponse(
            // 현재 버전이 최신 버전보다 낮으면 업데이트 필요
            isUpdateRequired = currentVersionCode < config.latestVersionCode,

            // 현재 버전이 최소 지원 버전보다 낮으면 강제 업데이트
            isForceUpdate = currentVersionCode < config.minVersionCode,

            latestVersionName = config.latestVersionName,
            updateMessage = config.updateMessage,
            storeUrl = config.storeUrl
        )
    }
}