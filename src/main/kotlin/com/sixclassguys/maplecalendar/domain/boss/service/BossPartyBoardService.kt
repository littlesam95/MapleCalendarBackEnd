package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardLikeRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoard
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoardImage
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoardLike
import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardImageRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardLikeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyMemberRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyRepository
import com.sixclassguys.maplecalendar.global.exception.BossPartyBoardNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyBoardUnauthorizedException
import com.sixclassguys.maplecalendar.global.exception.BossPartyMemberNotFoundException
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
    private val bossPartyMemberRepository: BossPartyMemberRepository,
    private val bossPartyBoardRepository: BossPartyBoardRepository,
    private val bossPartyBoardImageRepository: BossPartyBoardImageRepository,
    private val bossPartyBoardLikeRepository: BossPartyBoardLikeRepository,
    private val s3Service: S3Service
) {

    /**
     * 게시판 게시글 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun getBoardPosts(
        partyId: Long,
        userEmail: String,
        pageable: Pageable
    ): Slice<BossPartyBoardResponse> {
        // 1. 파티 존재 여부 확인
        bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // 2. 이 파티에 참여 중인 현재 유저의 캐릭터 식별
        // (내가 좋아요를 눌렀는지, 내 글인지 판단하기 위함)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyBoardUnauthorizedException()

        val currentCharacterId = partyMember.character.id

        // 3. 게시글 목록 조회
        val boards = bossPartyBoardRepository.findAllByBossPartyId(partyId, pageable)

        // 4. 응답 빌드 (식별한 내 캐릭터 ID 전달)
        return boards.map { board ->
            buildBoardResponse(board, currentCharacterId)
        }
    }

    /**
     * 게시글 작성
     */
    @Transactional
    fun createBoardPost(
        partyId: Long,
        userEmail: String, // 💡 characterId 대신 이메일 사용
        request: BossPartyBoardCreateRequest,
        imageFiles: List<MultipartFile>?
    ): BossPartyBoardResponse {
        val party = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // 1. 해당 파티에 가입된(ACCEPTED) 이 사용자의 캐릭터 정보를 조회
        val partyMember = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        // 초대한 상태(INVITED)인 경우 글 작성을 막으려면 체크 추가
        if (partyMember.joinStatus != JoinStatus.ACCEPTED) {
            throw BossPartyBoardUnauthorizedException("파티 수락 후 게시글을 작성할 수 있어요.")
        }

        val character = partyMember.character

        // 2. 게시글 생성
        val board = BossPartyBoard(
            bossParty = party,
            character = character,
            content = request.content
        )
        val savedBoard = bossPartyBoardRepository.save(board)

        // 3. 이미지 업로드 (S3)
        if (!imageFiles.isNullOrEmpty()) {
            imageFiles.filter { !it.isEmpty }.forEach { imageFile ->
                val imageUrl = s3Service.uploadFile(imageFile, "boss-party-board/$partyId")
                bossPartyBoardImageRepository.save(
                    BossPartyBoardImage(bossPartyBoard = savedBoard, imageUrl = imageUrl)
                )
            }
        }

        return buildBoardResponse(savedBoard, character.id)
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
        userEmail: String,
        request: BossPartyBoardLikeRequest
    ): BossPartyBoardResponse {
        val board = bossPartyBoardRepository.findByIdAndBossPartyId(boardId, partyId)
            ?: throw BossPartyBoardNotFoundException()

        val partyMember = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyBoardUnauthorizedException("파티 멤버만 좋아요를 누를 수 있어요.")

        val character = partyMember.character

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
