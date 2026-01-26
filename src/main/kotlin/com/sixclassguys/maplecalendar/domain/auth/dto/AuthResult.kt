package com.sixclassguys.maplecalendar.domain.auth.dto

import com.sixclassguys.maplecalendar.domain.member.entity.Member

data class AuthResult(
    val member: Member,
    val isNewMember: Boolean
)