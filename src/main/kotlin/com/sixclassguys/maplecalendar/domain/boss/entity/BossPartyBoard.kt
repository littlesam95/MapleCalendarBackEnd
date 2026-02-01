package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "boss_party_board")
class BossPartyBoard(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    // BossParty 객체와 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_party_id", nullable = false)
    val bossParty: BossParty,

    // Member 객체와 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(nullable = false, length = 1000)
    val content: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

) {

    @OneToMany(
        mappedBy = "bossPartyBoard",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val images: MutableList<BossPartyBoardImage> = mutableListOf()

    @OneToMany(
        mappedBy = "bossPartyBoard",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val likes: MutableList<BossPartyBoardLike> = mutableListOf()
}
