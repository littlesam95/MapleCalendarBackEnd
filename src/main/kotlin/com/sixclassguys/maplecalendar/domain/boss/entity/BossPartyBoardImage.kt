package com.sixclassguys.maplecalendar.domain.boss.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "boss_party_board_image")
class BossPartyBoardImage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_party_board_id", nullable = false)
    val bossPartyBoard: BossPartyBoard,

    @Column(nullable = false, length = 2000)
    val imageUrl: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)
