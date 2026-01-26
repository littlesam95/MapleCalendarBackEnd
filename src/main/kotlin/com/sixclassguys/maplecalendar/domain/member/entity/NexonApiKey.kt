package com.sixclassguys.maplecalendar.domain.member.entity

import com.sixclassguys.maplecalendar.global.config.ApiKeyConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "nexon_api_keys",
    indexes = [Index(name = "idx_api_key_hash", columnList = "api_key_hash")] // 빠른 조회를 위한 인덱스
)
class NexonApiKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(name = "nexon_api_key", unique = true, nullable = false, length = 1000)
    @Convert(converter = ApiKeyConverter::class) // 기존 암호화 컨버터 그대로 적용
    var nexonApiKey: String,

    @Column(name = "api_key_hash", unique = true, nullable = false, length = 512)
    var apiKeyHash: String // SHA-256 등 해싱된 값
)