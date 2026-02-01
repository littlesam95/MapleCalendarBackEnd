package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole
import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "boss_party_member",
    uniqueConstraints = [UniqueConstraint(columnNames = ["boss_party_id", "character_id"])]
)
class BossPartyMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(name = "boss_party_id", nullable = false)
//    val bossPartyId: Long,
//
//    @Column(name = "character_id", nullable = false)
//    val characterId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_party_id", nullable = false)
    val bossParty: BossParty,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    val character: MapleCharacter,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: PartyRole,

    @Enumerated(EnumType.STRING)
    @Column(name = "join_status")
    var joinStatus: JoinStatus? = null,

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime = LocalDateTime.now(),
)
