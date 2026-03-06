package com.sixclassguys.maplecalendar.domain.maplebgm.controller

import com.sixclassguys.maplecalendar.domain.maplebgm.dto.CreateMapleBgmPlaylistRequest
import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmPlaylistResponse
import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmPlaylistUpdateRequests
import com.sixclassguys.maplecalendar.domain.maplebgm.dto.MapleBgmResponse
import com.sixclassguys.maplecalendar.domain.maplebgm.enum.MapleBgmLikeStatus
import com.sixclassguys.maplecalendar.domain.maplebgm.service.MapleBgmService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/playlist")
class MapleBgmController(
    private val mapleBgmService: MapleBgmService
) {

    @GetMapping("/bgm/{bgmId}")
    fun getMapleBgmDetail(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bgmId: Long
    ): ResponseEntity<MapleBgmResponse> {
        val response = mapleBgmService.getMapleBgmDetail(userDetails.username, bgmId)

        return ResponseEntity.ok(response)
    }

    // 1. 인기 차트 조회
    @GetMapping("/top")
    fun getTopBgms(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Slice<MapleBgmResponse>> {
        val response = mapleBgmService.getTopBgms(userDetails.username, page, size)

        return ResponseEntity.ok(response)
    }

    // 2. 최신곡 조회
    @GetMapping("/recent")
    fun getRecentBgms(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Slice<MapleBgmResponse>> {
        val response = mapleBgmService.getRecentBgms(userDetails.username, page, size)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/search")
    fun searchBgms(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Slice<MapleBgmResponse>> {
        val response = mapleBgmService.searchBgms(userDetails.username, query, page, size)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/bgm/{bgmId}/like")
    fun toggleLike(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable bgmId: Long,
        @RequestParam status: MapleBgmLikeStatus
    ): ResponseEntity<MapleBgmResponse> {
        val response = mapleBgmService.toggleReaction(userDetails.username, bgmId, status)

        return ResponseEntity.ok(response)
    }

    // 3. 내 플레이리스트 목록 조회
    @GetMapping("/mylists")
    fun getMyPlaylists(
        @AuthenticationPrincipal userDetails: UserDetails,
    ): ResponseEntity<List<MapleBgmPlaylistResponse>> {
        val response = mapleBgmService.getMyPlaylists(userDetails.username)

        return ResponseEntity.ok(response)
    }

    // 플레이리스트 상세 조회
    @GetMapping("/mylists/{playlistId}")
    fun getPlaylistDetail(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable playlistId: Long
    ): ResponseEntity<MapleBgmPlaylistResponse> {
        val response = mapleBgmService.getPlaylistDetail(userDetails.username, playlistId)

        return ResponseEntity.ok(response)
    }

    // 4. 플레이리스트 생성
    @PostMapping("/mylists")
    fun createPlaylist(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CreateMapleBgmPlaylistRequest
    ): ResponseEntity<List<MapleBgmPlaylistResponse>> {
        val response = mapleBgmService.createPlaylist(userDetails.username, request.name, request.isPublic)

        return ResponseEntity.ok(response)
    }

    // 5. 플레이리스트 삭제
    @DeleteMapping("/mylists/{playlistId}")
    fun deletePlaylist(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable playlistId: Long
    ): ResponseEntity<List<MapleBgmPlaylistResponse>> {
        val response = mapleBgmService.deletePlaylist(userDetails.username, playlistId)

        return ResponseEntity.ok(response)
    }

    // 6. 플레이리스트에 곡 추가
    @PostMapping("/mylists/{playlistId}/bgms/{bgmId}")
    fun addBgmToPlaylist(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable playlistId: Long,
        @PathVariable bgmId: Long
    ): ResponseEntity<MapleBgmPlaylistResponse> {
        val response = mapleBgmService.addBgmToPlaylist(userDetails.username, playlistId, bgmId)

        return ResponseEntity.ok(response)
    }

    // 7. 플레이리스트에서 곡 제거
    @DeleteMapping("/mylists/{playlistId}/bgms/{bgmId}")
    fun removeBgmFromPlaylist(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable playlistId: Long,
        @PathVariable bgmId: Long
    ): ResponseEntity<MapleBgmPlaylistResponse> {
        val response = mapleBgmService.removeBgmFromPlaylist(userDetails.username, playlistId, bgmId)

        return ResponseEntity.ok(response)
    }

    // 8 & 9. 플레이리스트 편집 및 순서 변경
    @PatchMapping("/mylists/{playlistId}")
    fun updatePlaylist(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable playlistId: Long,
        @RequestBody request: MapleBgmPlaylistUpdateRequests
    ): ResponseEntity<MapleBgmPlaylistResponse> {
        val response = mapleBgmService.updatePlaylist(userDetails.username, playlistId, request)

        return ResponseEntity.ok(response)
    }
}