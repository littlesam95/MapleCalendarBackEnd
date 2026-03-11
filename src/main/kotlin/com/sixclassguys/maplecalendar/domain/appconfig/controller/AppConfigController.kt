package com.sixclassguys.maplecalendar.domain.appconfig.controller

import com.sixclassguys.maplecalendar.domain.appconfig.dto.AppVersionResponse
import com.sixclassguys.maplecalendar.domain.appconfig.service.AppConfigService
import com.sixclassguys.maplecalendar.domain.notification.dto.Platform
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/app-config")
class AppConfigController(
    private val appConfigService: AppConfigService
) {

    @GetMapping("/version-check")
    fun checkVersion(
        @RequestParam platform: Platform, // "ANDROID" 또는 "IOS"
        @RequestParam versionCode: Int
    ): AppVersionResponse {
        return appConfigService.getUpdateInfo(platform, versionCode)
    }
}