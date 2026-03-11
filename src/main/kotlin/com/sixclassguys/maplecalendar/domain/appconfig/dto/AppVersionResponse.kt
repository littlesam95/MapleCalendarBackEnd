package com.sixclassguys.maplecalendar.domain.appconfig.dto

data class AppVersionResponse(
    val isUpdateRequired: Boolean, // 업데이트 팝업을 띄워야 하는가?
    val isForceUpdate: Boolean,   // 강제 업데이트인가? (취소 불가)
    val latestVersionName: String,
    val updateMessage: String,
    val storeUrl: String
)