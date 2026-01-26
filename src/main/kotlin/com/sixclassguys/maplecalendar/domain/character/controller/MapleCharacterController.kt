package com.sixclassguys.maplecalendar.domain.character.controller

import com.sixclassguys.maplecalendar.domain.character.service.MapleCharacterService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/character")
class MapleCharacterController(
    private val mapleCharacterService: MapleCharacterService
) {


}