package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardLikeRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoard
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoardImage
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoardLike
import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardImageRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardLikeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyRepository
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.service.S3Service
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class BossPartyBoardService(
    private val bossPartyRepository: BossPartyRepository,
    private val bossPartyBoardRepository: BossPartyBoardRepository,
    private val bossPartyBoardImageRepository: BossPartyBoardImageRepository,
    private val bossPartyBoardLikeRepository: BossPartyBoardLikeRepository,
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val s3Service: S3Service
) {

    /**
     * 게시판 게시글 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun getBoardPosts(
        partyId: Long,
        characterId: Long,
        pageable: Pageable
    ): Slice<BossPartyBoardResponse> {
        // 파티 존재 여부 확인
        bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        val boards = bossPartyBoardRepository.findAllByBossPartyId(partyId, pageable)

        return boards.map { board ->
            buildBoardResponse(board, characterId)
        }
    }

    /**
     * 게시글 작성
     */
    @Transactional
    fun createBoardPost(
        partyId: Long,
        characterId: Long,
        request: BossPartyBoardCreateRequest,
        imageFiles: List<MultipartFile>?
    ): BossPartyBoardResponse {
        val party = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // characterId로 직접 조회
        val character = mapleCharacterRepository.findById(characterId)
            .orElseThrow { IllegalArgumentException("캐릭터를 찾을 수 없습니다.") }

        // 1. 게시글 생성
        val board = BossPartyBoard(
            bossParty = party,
            character = character,
            content = request.content
        )
        val savedBoard = bossPartyBoardRepository.save(board)

        // 2. 이미지 업로드
        if (!imageFiles.isNullOrEmpty()) {
            imageFiles.forEach { imageFile ->
                if (!imageFile.isEmpty) {
                    try {
                        val imageUrl = s3Service.uploadFile(imageFile, "boss-party-board/$partyId")
                        val boardImage = BossPartyBoardImage(
                            bossPartyBoard = savedBoard,
                            imageUrl = imageUrl
                        )
                        bossPartyBoardImageRepository.save(boardImage)
                    } catch (e: Exception) {
                        throw RuntimeException("이미지 업로드 실패: ${e.message}", e)
                    }
                }
            }
        }

        return buildBoardResponse(savedBoard, character.member.id)
    }


    /**
     * 게시글 응답 빌더
     */
    private fun buildBoardResponse(
        board: BossPartyBoard,
        currentCharacterId: Long
    ): BossPartyBoardResponse {
        val images = bossPartyBoardImageRepository.findAllByBossPartyBoardId(board.id)
        val likeCount = bossPartyBoardLikeRepository
            .countByBossPartyBoardIdAndBoardLikeType(board.id, BoardLikeType.LIKE)
        val dislikeCount = bossPartyBoardLikeRepository
            .countByBossPartyBoardIdAndBoardLikeType(board.id, BoardLikeType.DISLIKE)
        val userLike = bossPartyBoardLikeRepository
            .findByBossPartyBoardIdAndCharacterId(board.id, currentCharacterId)

        return BossPartyBoardResponse(
            id = board.id,
            characterId = board.character.id,
            characterName = board.character.characterName,
            characterImage = board.character.characterImage,
            characterClass = board.character.characterClass,
            characterLevel = board.character.characterLevel,
            content = board.content,
            createdAt = board.createdAt,
            imageUrls = images.map { it.imageUrl },
            likeCount = likeCount.toInt(),
            dislikeCount = dislikeCount.toInt(),
            userLikeType = userLike?.boardLikeType?.toString(),
            isAuthor = board.character.member.id == currentCharacterId
        )
    }

    /**
     * 게시글에 좋아요/싫어요 토글
     */
    @Transactional
    fun toggleBoardLike(
        partyId: Long,
        boardId: Long,
        characterId: Long,
        request: BossPartyBoardLikeRequest
    ): BossPartyBoardResponse {
        val board = bossPartyBoardRepository.findByIdAndBossPartyId(boardId, partyId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다.")

        val character = mapleCharacterRepository.findById(characterId)
            .orElseThrow { IllegalArgumentException("캐릭터를 찾을 수 없습니다.") }

        // 기존 좋아요 확인
        val existingLike = bossPartyBoardLikeRepository
            .findByBossPartyBoardIdAndCharacterId(boardId, character.id)

        if (existingLike != null) {
            if (existingLike.boardLikeType == request.boardLikeType) {
                bossPartyBoardLikeRepository.delete(existingLike)
            } else {
                existingLike.boardLikeType = request.boardLikeType
            }
        } else {
            val newLike = BossPartyBoardLike(
                bossPartyBoard = board,
                character = character,
                boardLikeType = request.boardLikeType
            )
            bossPartyBoardLikeRepository.save(newLike)
        }

        return buildBoardResponse(board, character.id)
    }
}
