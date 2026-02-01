package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "boss_party_board_like",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["boss_party_board_id", "member_id"]
        )
    ]
)
class BossPartyBoardLike(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_party_board_id", nullable = false)
    val bossPartyBoard: BossPartyBoard,

    @Column(nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val boardLikeType: BoardLikeType,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
