package com.sixclassguys.maplecalendar.domain.util

enum class MapleWorld(
    val worldName: String
) {
    SCANIA("스카니아"),
    BERA("베라"),
    LUNA("루나"),
    ZENITH("제니스"),
    CROA("크로아"),
    UNION("유니온"),
    ELYSIUM("엘리시움"),
    ENOSIS("이노시스"),
    RED("레드"),
    AURORA("오로라"),
    ARCANE("아케인"),
    NOVA("노바"),
    CHALLENGERS("챌린저스"),
    CHALLENGERS2("챌린저스2"),
    CHALLENGERS3("챌린저스3"),
    CHALLENGERS4("챌린저스4"),
    EOS("에오스"),
    HELIOS("핼리오스");

    companion object {

        // 이름으로 Enum을 찾는 메서드 (필터링의 핵심)
        fun fromName(name: String): MapleWorld? {
            return entries.find { it.worldName == name }
        }
    }
}

enum class WorldGroup(
    val title: String
) {
    NORMAL("일반 월드"),
    EOS_HELIOS("에오스/핼리오스"),
    CHALLENGERS("챌린저스");

    companion object {

        fun classify(world: MapleWorld): WorldGroup {
            return when (world) {
                MapleWorld.EOS, MapleWorld.HELIOS -> EOS_HELIOS
                MapleWorld.CHALLENGERS, MapleWorld.CHALLENGERS2,
                MapleWorld.CHALLENGERS3, MapleWorld.CHALLENGERS4 -> CHALLENGERS
                else -> NORMAL
            }
        }
    }
}