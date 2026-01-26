package com.sixclassguys.maplecalendar.domain.character.dto

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter

data class MapleCharacterListResponse(
    val groupedCharacters: Map<String, Map<String, List<MapleCharacter>>>
)