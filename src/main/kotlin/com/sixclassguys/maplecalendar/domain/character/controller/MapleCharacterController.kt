package com.sixclassguys.maplecalendar.domain.character.controller

import com.sixclassguys.maplecalendar.domain.character.dto.CharacterAuthorityResponse
import com.sixclassguys.maplecalendar.domain.character.dto.CharacterRegisterRequest
import com.sixclassguys.maplecalendar.domain.character.dto.MapleCharacterListResponse
import com.sixclassguys.maplecalendar.domain.character.service.MapleCharacterService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/character")
class MapleCharacterController(
    private val mapleCharacterService: MapleCharacterService
) {

    @GetMapping("/list")
    fun getCharacters(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<MapleCharacterListResponse> {
        val response = mapleCharacterService.getGroupedCharacters(userDetails.username)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/fetch")
    suspend fun fetchFromNexon(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestHeader("x-nxopen-api-key") apiKey: String
    ): ResponseEntity<MapleCharacterListResponse> {
        val response = mapleCharacterService.fetchCharactersFromNexon(userDetails.username, apiKey)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    suspend fun registerCharacters(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CharacterRegisterRequest
    ): ResponseEntity<MapleCharacterListResponse> {
        // 등록과 동시에 최신 그룹화 목록을 받아옴
        val response = mapleCharacterService.registerAndGetList(userDetails.username, request.ocids)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{ocid}/check-authority")
    fun checkAuthority(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable ocid: String
    ): ResponseEntity<CharacterAuthorityResponse> {
        // Controller -> Service (의존성 규칙 준수)
        val response = mapleCharacterService.getCharacterAuthority(userDetails.username, ocid)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{ocid}/representative")
    fun setRepresentative(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable ocid: String
    ): ResponseEntity<Unit> {
        mapleCharacterService.updateRepresentativeCharacter(userDetails.username, ocid)

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{ocid}")
    fun deleteCharacter(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable ocid: String
    ): ResponseEntity<Unit> {
        mapleCharacterService.deleteCharacter(userDetails.username, ocid)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun searchCharacters(
        @RequestParam name: String
    ): ResponseEntity<MapleCharacterListResponse> {
        val response = mapleCharacterService.searchCharactersByName(name)
        return ResponseEntity.ok(response)
    }
}