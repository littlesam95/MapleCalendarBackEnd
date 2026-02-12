package com.sixclassguys.maplecalendar.domain.boss.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardLikeRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyBoardResponse
import com.sixclassguys.maplecalendar.domain.boss.enums.BoardLikeType
import com.sixclassguys.maplecalendar.domain.boss.service.BossPartyBoardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Boss Party Board API", description = "보스 파티 게시판 관련 API")
@RestController
@RequestMapping("/api/boss-parties/{partyId}/board")
class BossPartyBoardController(
    private val boardService: BossPartyBoardService,
    private val objectMapper: ObjectMapper
) {

    @Operation(summary = "게시판 게시글 목록 조회", description = "모든 게시글을 페이징으로 조회합니다.")
    @GetMapping
    fun getBoardPosts(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable partyId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "5") size: Int
    ): ResponseEntity<Slice<BossPartyBoardResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val posts = boardService.getBoardPosts(partyId, userDetails.username, pageable)
        return ResponseEntity.ok(posts)
    }

    @Schema(name = "BossPartyBoardCreateMultipart")
    data class BossPartyBoardCreateMultipart(
        @field:Schema(
            description = "게시글 JSON",
            implementation = BossPartyBoardCreateRequest::class
        )
        val content: BossPartyBoardCreateRequest? = null,

        @field:ArraySchema(
            arraySchema = Schema(description = "이미지 파일들"),
            schema = Schema(type = "string", format = "binary")
        )
        val images: List<MultipartFile>? = null
    )

    @Operation(
        summary = "게시글 작성",
        description = "텍스트(content JSON) + 이미지(images) 멀티파트 업로드",
        requestBody = RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = Schema(implementation = BossPartyBoardCreateMultipart::class),
                    encoding = [
                        Encoding(name = "content", contentType = "application/json")
                    ]
                )
            ]
        )
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createBoardPost(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable partyId: Long,
        @RequestPart("content") content: String,
        @RequestPart(value = "images", required = false) imageFiles: List<MultipartFile>?
    ): ResponseEntity<BossPartyBoardResponse> {
        val request = objectMapper.readValue(content, BossPartyBoardCreateRequest::class.java)

        val response = boardService.createBoardPost(partyId, userDetails.username, request, imageFiles)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{boardId}/like")
    fun toggleBoardLike(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable partyId: Long,
        @PathVariable boardId: Long,
        @RequestParam likeType: String
    ): ResponseEntity<BossPartyBoardResponse> {
        return try {
            val likeEnum = try {
                BoardLikeType.valueOf(likeType.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

            val request = BossPartyBoardLikeRequest(boardLikeType = likeEnum)
            val response = boardService.toggleBoardLike(partyId, boardId, userDetails.username, request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

}
