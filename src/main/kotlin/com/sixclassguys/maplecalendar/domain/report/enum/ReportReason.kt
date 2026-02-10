package com.sixclassguys.maplecalendar.domain.report.enum

enum class ReportReason(val description: String) {

    ABUSE("욕설 및 비하 발언"),
    SPAM("도배성 메시지"),
    ADVERTISING("부적절한 홍보/광고"),
    SEXUAL_HARASSMENT("성희롱 또는 부적절한 성적 표현"),
    INAPPROPRIATE_CONTENT("게임 가이드라인 위반 콘텐츠"),
    OTHER("기타 (직접 입력)")
}