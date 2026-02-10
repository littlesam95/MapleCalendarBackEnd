package com.sixclassguys.maplecalendar.domain.boss.entity

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.boss.enums.BossPartyChatMessageType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "boss_party_chat_messages")
class BossPartyChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_party_id", nullable = false)
    val bossParty: BossParty, // BossParty와의 다대일 관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    val character: MapleCharacter, // MapleCharacter와의 다대일 관계

    @Column(nullable = false, length = 500)
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    var messageType: BossPartyChatMessageType, // TALK, JOIN, LEAVE 등을 구분하는 ENUM

    @Column(nullable = false)
    var isHidden: Boolean = false,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false, // 삭제 여부 플래그 추가

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    // 메시지 삭제 상태로 변경하는 편의 메서드
    fun markAsDeleted() {
        this.isDeleted = true
        this.content = "이 메시지는 삭제되었어요."
    }

    fun hide() {
        this.isHidden = true
    }
}