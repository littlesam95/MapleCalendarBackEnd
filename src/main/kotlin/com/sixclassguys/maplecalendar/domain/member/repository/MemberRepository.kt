package com.sixclassguys.maplecalendar.domain.member.repository

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    // API Key를 기반으로 유저를 조회하는 쿼리 메소드
    fun findByNexonApiKey(nexonApiKey: String): Member?
}