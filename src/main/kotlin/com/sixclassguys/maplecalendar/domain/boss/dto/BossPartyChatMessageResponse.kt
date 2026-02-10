package com.sixclassguys.maplecalendar.domain.boss.dto

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import com.sixclassguys.maplecalendar.domain.boss.enums.BossPartyChatMessageType

data class BossPartyChatMessageResponse(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val senderWorld: String,
    val senderImage: String?,
    val content: String,
    val messageType: BossPartyChatMessageType,
    val isMine: Boolean, // 조회하는 사람의 ID와 비교하여 서버에서 계산해서 전달
    val isDeleted: Boolean,
    val isHidden: Boolean,
    val createdAt: String
)

// 엔터티를 DTO로 변환하는 확장 함수
fun BossPartyChatMessage.toResponse(currentCharacterId: Long) = BossPartyChatMessageResponse(
    id = this.id,
    senderId = this.character.id,
    senderName = this.character.characterName,
    senderWorld = this.character.worldName,
    senderImage = this.character.characterImage,
    content = this.content,
    messageType = this.messageType,
    isMine = this.character.id == currentCharacterId,
    isDeleted = this.isDeleted,
    isHidden = this.isHidden,
    createdAt = this.createdAt.toString()
)