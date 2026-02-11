package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType
import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "boss_party_board_like",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["boss_party_board_id", "character_id"]
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    val character: MapleCharacter,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var boardLikeType: BoardLikeType,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
