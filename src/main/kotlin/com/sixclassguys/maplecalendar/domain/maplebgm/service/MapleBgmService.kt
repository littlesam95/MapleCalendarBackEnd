package com.sixclassguys.maplecalendar.domain.maplebgm.service

import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmPlaylistResponse
import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmPlaylistUpdateRequests
import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmResponse
import com.sixclassguys.maplecalendar.domain.maplebgm.entity.MapleBgm
import com.sixclassguys.maplecalendar.domain.maplebgm.entity.MapleBgmLike
import com.sixclassguys.maplecalendar.domain.maplebgm.entity.MapleBgmPlaylist
import com.sixclassguys.maplecalendar.domain.maplebgm.entity.MapleBgmPlaylistItem
import com.sixclassguys.maplecalendar.domain.maplebgm.enum.MapleBgmLikeStatus
import com.sixclassguys.maplecalendar.domain.maplebgm.repository.MapleBgmLikeRepository
import com.sixclassguys.maplecalendar.domain.maplebgm.repository.MapleBgmPlaylistItemRepository
import com.sixclassguys.maplecalendar.domain.maplebgm.repository.MapleBgmPlaylistRepository
import com.sixclassguys.maplecalendar.domain.maplebgm.repository.MapleBgmRepository
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.exception.DuplicateMapleBgmException
import com.sixclassguys.maplecalendar.global.exception.MapleBgmNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MapleBgmNotFoundInPlaylistException
import com.sixclassguys.maplecalendar.global.exception.MapleBgmPlaylistNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MapleBgmPlaylistUnauthorizedException
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MapleBgmService(
    private val memberRepository: MemberRepository,
    private val mapleBgmRepository: MapleBgmRepository,
    private val mapleBgmLikeRepository: MapleBgmLikeRepository,
    private val mapleBgmPlaylistRepository : MapleBgmPlaylistRepository,
    private val mapleBgmPlaylistItemRepository : MapleBgmPlaylistItemRepository,
) {

    private val baseUrl = "http://52.78.54.150//static/bgm/"

    private fun mergeUserStatus(member: Member, slice: Slice<MapleBgm>): Slice<MapleBgmResponse> {
        val bgmIds = slice.content.map { it.id }

        // 1. 해당 유저가 이 페이지의 곡들에 남긴 모든 반응(LIKE/DISLIKE)을 가져와서 Map으로 가공
        val reactionMap = mapleBgmLikeRepository.findAllByMemberAndMapleBgmIdIn(member, bgmIds)
            .associate { it.mapleBgm.id to it.status }

        return slice.map { bgm ->
            MapleBgmResponse(
                id = bgm.id,
                title = bgm.title,
                audioUrl = "$baseUrl${bgm.fileName}",
                mapName = bgm.mapName,
                region = bgm.region,
                likeCount = bgm.likeCount,
                dislikeCount = bgm.dislikeCount,
                likeStatus = reactionMap[bgm.id],
                playCount = bgm.totalPlayCount,
                description = bgm.description,
                thumbnailUrl = bgm.thumbnailUrl
            )
        }
    }

    fun convertToResponses(member: Member, bgms: List<MapleBgm>): List<MapleBgmResponse> {
        if (bgms.isEmpty()) return emptyList()

        val bgmIds = bgms.map { it.id }
        val reactionMap = mapleBgmLikeRepository.findAllByMemberAndMapleBgmIdIn(member, bgmIds)
            .associate { it.mapleBgm.id to it.status }

        return bgms.map { bgm ->
            MapleBgmResponse(
                id = bgm.id,
                title = bgm.title,
                audioUrl = "$baseUrl${bgm.fileName}",
                mapName = bgm.mapName,
                region = bgm.region,
                likeCount = bgm.likeCount,
                dislikeCount = bgm.dislikeCount,
                likeStatus = reactionMap[bgm.id],
                playCount = bgm.totalPlayCount,
                description = bgm.description,
                thumbnailUrl = bgm.thumbnailUrl
            )
        }
    }

    private fun updateCount(bgm: MapleBgm, status: MapleBgmLikeStatus, amount: Int) {
        when (status) {
            MapleBgmLikeStatus.LIKE -> {
                bgm.likeCount = (bgm.likeCount + amount).coerceAtLeast(0)
            }
            MapleBgmLikeStatus.DISLIKE -> {
                bgm.dislikeCount = (bgm.dislikeCount + amount).coerceAtLeast(0)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getMapleBgmDetail(userEmail: String, bgmId: Long): MapleBgmResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()
        val bgm = mapleBgmRepository.findById(bgmId).orElseThrow { MapleBgmNotFoundException() }
        val existingLike = mapleBgmLikeRepository.findByMemberAndMapleBgm(member, bgm)
        val reaction = mapleBgmLikeRepository.findByMemberAndMapleBgm(member, bgm)?.status

        return MapleBgmResponse(
            id = bgm.id,
            title = bgm.title,
            audioUrl = "$baseUrl${bgm.fileName}",
            mapName = bgm.mapName,
            region = bgm.region,
            likeCount = bgm.likeCount,
            dislikeCount = bgm.dislikeCount,
            likeStatus = reaction,
            playCount = bgm.totalPlayCount,
            description = bgm.description,
            thumbnailUrl = bgm.thumbnailUrl
        )
    }

    @Transactional(readOnly = true)
    fun searchBgms(userEmail: String, query: String, page: Int, size: Int): Slice<MapleBgmResponse> {
        val member = memberRepository.findByEmail(userEmail) ?: throw MemberNotFoundException()
        val pageable = PageRequest.of(page, size)

        // 제목 또는 맵 이름 둘 중 하나라도 query를 포함하면 결과에 포함
        val bgmSlice = mapleBgmRepository.findAllByTitleContainingOrMapNameContainingOrderByIdDesc(
            query, query, pageable
        )

        return mergeUserStatus(member, bgmSlice)
    }

    @Transactional(readOnly = true)
    fun getTopBgms(userEmail: String, page: Int, size: Int): Slice<MapleBgmResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val pageable: Pageable = PageRequest.of(page, size)
        val bgmSlice = mapleBgmRepository.findAllByOrderByLikeCountDescIdDesc(pageable)

        return mergeUserStatus(member, bgmSlice)
    }

    @Transactional(readOnly = true)
    fun getRecentBgms(userEmail: String, page: Int, size: Int): Slice<MapleBgmResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val pageable: Pageable = PageRequest.of(page, size)
        val bgmSlice = mapleBgmRepository.findAllByOrderByIdDesc(pageable)

        return mergeUserStatus(member, bgmSlice)
    }

    @Transactional
    fun toggleReaction(userEmail: String, bgmId: Long, newStatus: MapleBgmLikeStatus): MapleBgmResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val bgm = mapleBgmRepository.findById(bgmId).orElseThrow { MapleBgmNotFoundException() }
        val existingLike = mapleBgmLikeRepository.findByMemberAndMapleBgm(member, bgm)

        if (existingLike == null) {
            // 1. 기록이 전혀 없는 경우: 새로 생성
            mapleBgmLikeRepository.save(MapleBgmLike(member = member, mapleBgm = bgm, status = newStatus))
            updateCount(bgm, newStatus, 1)
        } else if (existingLike.status == newStatus) {
            // 2. 같은 버튼을 다시 누른 경우: 취소(삭제)
            mapleBgmLikeRepository.delete(existingLike)
            updateCount(bgm, newStatus, -1)
        } else {
            // 3. 다른 버튼을 누른 경우: 상태 변경 (예: LIKE -> DISLIKE)
            val oldStatus = existingLike.status
            existingLike.status = newStatus // Dirty Checking으로 업데이트
            updateCount(bgm, oldStatus, -1) // 이전 상태 카운트 감소
            updateCount(bgm, newStatus, 1)  // 새로운 상태 카운트 증가
        }

        val reaction = mapleBgmLikeRepository.findByMemberAndMapleBgm(member, bgm)?.status

        return MapleBgmResponse(
            id = bgm.id,
            title = bgm.title,
            audioUrl = "$baseUrl${bgm.fileName}",
            mapName = bgm.mapName,
            region = bgm.region,
            likeCount = bgm.likeCount,
            dislikeCount = bgm.dislikeCount,
            likeStatus = reaction,
            playCount = bgm.totalPlayCount,
            description = bgm.description,
            thumbnailUrl = bgm.thumbnailUrl
        )
    }

    // 1. 내 플레이리스트 전체 목록 조회
    @Transactional(readOnly = true)
    fun getMyPlaylists(userEmail: String): List<MapleBgmPlaylistResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val playlists = mapleBgmPlaylistRepository.findAllByMemberOrderByCreatedAtDesc(member)

        return playlists.map { playlist ->
            // sortOrder 순으로 정렬된 아이템 중 상위 4개만 가져옴
            val activeItems = playlist.items.filter { !it.isDeleted }
            val top4Urls = activeItems.sortedBy { it.sortOrder }
                .take(4)
                .map { it.bgm }

            MapleBgmPlaylistResponse(
                id = playlist.id,
                name = playlist.name,
                description = null,
                isPublic = playlist.isPublic,
                bgms = convertToResponses(member, top4Urls)
            )
        }
    }

    // 2. 특정 플레이리스트 상세 조회 (곡 목록 포함)
    @Transactional(readOnly = true)
    fun getPlaylistDetail(userEmail: String, playlistId: Long): MapleBgmPlaylistResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val playlist = mapleBgmPlaylistRepository.findById(playlistId)
            .orElseThrow { MapleBgmPlaylistNotFoundException() }

        // 본인 소유이거나 공개된 플레이리스트인 경우에만 조회 가능
        if (!playlist.isPublic && playlist.member.id != member.id) {
            throw MapleBgmPlaylistUnauthorizedException()
        }

        // sortOrder 순으로 정렬하여 BGM 리스트 생성
        val activeBgms = playlist.items
            .filter { !it.isDeleted }
            .sortedBy { it.sortOrder }
            .map { it.bgm }

        val bgmResponses = convertToResponses(member, activeBgms)

        return MapleBgmPlaylistResponse(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            isPublic = playlist.isPublic,
            bgms = bgmResponses
        )
    }

    // 1. 플레이리스트 생성
    @Transactional
    fun createPlaylist(userEmail: String, name: String, isPublic: Boolean): List<MapleBgmPlaylistResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val newPlaylist = MapleBgmPlaylist(name = name, member = member, isPublic = isPublic)
        mapleBgmPlaylistRepository.save(newPlaylist).id

        return getMyPlaylists(userEmail)
    }

    @Transactional
    fun deletePlaylist(userEmail: String, playlistId: Long): List<MapleBgmPlaylistResponse> {
        val member = memberRepository.findByEmail(userEmail) ?: throw MemberNotFoundException()
        val playlist = mapleBgmPlaylistRepository.findById(playlistId).orElseThrow()

        if (playlist.member.id != member.id) {
            throw MapleBgmPlaylistUnauthorizedException("내 플레이리스트가 아니에요.")
        }

        mapleBgmPlaylistRepository.delete(playlist)

        return getMyPlaylists(userEmail)
    }

    // 2. 플레이리스트에 곡 추가
    @Transactional
    fun addBgmToPlaylist(userEmail: String, playlistId: Long, bgmId: Long): MapleBgmPlaylistResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val playlist = mapleBgmPlaylistRepository.findById(playlistId).orElseThrow()

        // 본인 소유 확인
        if (playlist.member.id != member.id) {
            throw MapleBgmPlaylistUnauthorizedException("내 플레이리스트가 아니에요.")
        }

        val bgm = mapleBgmRepository.findById(bgmId).orElseThrow()

        // 중복 추가 방지
        if (mapleBgmPlaylistItemRepository.existsByPlaylistAndBgm(playlist, bgm)) throw DuplicateMapleBgmException()

        // 마지막 순서 계산 (가장 뒤에 추가)
        val lastOrder = playlist.items.maxOfOrNull { it.sortOrder } ?: -1

        val newItem = MapleBgmPlaylistItem(
            playlist = playlist,
            bgm = bgm,
            sortOrder = lastOrder + 1
        )
        mapleBgmPlaylistItemRepository.save(newItem)

        return getPlaylistDetail(userEmail, playlistId)
    }

    // 3. 플레이리스트에서 곡 제거
    @Transactional
    fun removeBgmFromPlaylist(userEmail: String, playlistId: Long, bgmId: Long): MapleBgmPlaylistResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val playlist = mapleBgmPlaylistRepository.findById(playlistId).orElseThrow()
        if (playlist.member.id != member.id) {
            throw MapleBgmPlaylistUnauthorizedException("내 플레이리스트가 아니에요.")
        }

        val bgm = mapleBgmRepository.findById(bgmId)
            .orElseThrow { MapleBgmNotFoundException() }

        val item = mapleBgmPlaylistItemRepository.findByPlaylistAndBgmAndIsDeletedFalse(playlist, bgm)
            ?: throw MapleBgmNotFoundInPlaylistException()

        // 1. 논리적 삭제 처리
        item.isDeleted = true
        item.deletedAt = LocalDateTime.now()

        // 2. [중요] 뒤에 있는 곡들의 순서(sortOrder)를 앞으로 한 칸씩 당겨주기
        // 이 처리를 안 하면 순서에 구멍(0, 1, 3, 4...)이 생깁니다.
        val remainingItems = mapleBgmPlaylistItemRepository.findAllByPlaylistAndIsDeletedFalseOrderBySortOrderAsc(playlist)
        remainingItems.forEachIndexed { index, remainingItem ->
            remainingItem.sortOrder = index
        }

        return getPlaylistDetail(userEmail, playlistId)
    }

    @Transactional
    fun updatePlaylist(userEmail: String, playlistId: Long, request: MapleBgmPlaylistUpdateRequests): MapleBgmPlaylistResponse {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val playlist = mapleBgmPlaylistRepository.findById(playlistId).orElseThrow { MapleBgmPlaylistNotFoundException() }

        // 1. 권한 체크
        if (playlist.member.id != member.id) {
            throw MapleBgmPlaylistUnauthorizedException("내 플레이리스트가 아니에요.")
        }

        // 2. 기본 정보 수정 (이름, 공개여부)
        request.name?.let { playlist.name = it }
        request.isPublic?.let { playlist.isPublic = it }

        // 3. 순서(sortOrder) 업데이트
        // 현재 플레이리스트에 담긴 아이템들을 Map(BGM_ID to Item)으로 변환
        val activeItemMap = playlist.items.filter { !it.isDeleted }.associateBy { it.bgm.id }

        // 유저가 보낸 ID 순서대로 sortOrder 재할당
        request.bgmIdOrder.forEachIndexed { index, bgmId ->
            activeItemMap[bgmId]?.let { item ->
                item.sortOrder = index
            }
        }

        return getPlaylistDetail(userEmail, playlistId)
    }
}