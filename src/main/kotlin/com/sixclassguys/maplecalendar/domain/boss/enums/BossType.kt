package com.sixclassguys.maplecalendar.domain.boss.enums

enum class BossType(
    val bossName: String,
    val difficulties: List<BossDifficulty>,
    val memberCounts: List<Int>
) {
    // 그란디스
    SEREN("선택받은 세렌",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD, BossDifficulty.EXTREME),
        listOf(6, 6, 6)),
    KALOS("감시자 칼로스",
        listOf(BossDifficulty.EASY, BossDifficulty.NORMAL, BossDifficulty.CHAOS, BossDifficulty.EXTREME),
        listOf(6, 6, 6, 6)),
    THEFIRSTADVERSARY("최초의 대적자",
        listOf(BossDifficulty.EASY, BossDifficulty.NORMAL, BossDifficulty.HARD, BossDifficulty.EXTREME),
        listOf(3, 3, 3, 3)),
    KALING("카링",
        listOf(BossDifficulty.EASY, BossDifficulty.NORMAL, BossDifficulty.HARD, BossDifficulty.EXTREME),
        listOf(6, 6, 6, 6)),
    RADIANTMALEFIC("찬란한 흉성",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(3, 3)),
    LIMBO("림보",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(3, 3)),
    BALDRIX("발드릭스",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(3, 3)),
    JUPITER("유피테르",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(3, 3)),

    // 아케인리버
    LUCID("루시드",
        listOf(BossDifficulty.EASY, BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(6, 6, 6)),
    WILL("윌",
        listOf(BossDifficulty.EASY, BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(6, 6, 6)),
    DUSK("더스크",
        listOf(BossDifficulty.NORMAL, BossDifficulty.CHAOS),
        listOf(6, 6)),
    VERUSHILLA("진 힐라",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(6, 6)),
    DUNKEL("듄켈",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(6, 6)),
    BLACKMAGE("검은 마법사",
        listOf(BossDifficulty.HARD, BossDifficulty.EXTREME),
        listOf(6, 6)),

    // 메이플 월드
    ZAKUM("자쿰", listOf(BossDifficulty.CHAOS), listOf(6)),
    MAGNUS("매그너스", listOf(BossDifficulty.HARD), listOf(6)),
    HILLA("힐라", listOf(BossDifficulty.HARD), listOf(6)),
    PAPULATUS("파풀라투스", listOf(BossDifficulty.CHAOS), listOf(6)),
    VONBON("반반", listOf(BossDifficulty.CHAOS), listOf(6)),
    PIERRE("피에르", listOf(BossDifficulty.CHAOS), listOf(6)),
    BLOODYQUEEN("블러디 퀸", listOf(BossDifficulty.CHAOS), listOf(6)),
    VELLUM("벨룸", listOf(BossDifficulty.CHAOS), listOf(6)),
    PINKBEAN("핑크빈", listOf(BossDifficulty.CHAOS), listOf(6)),
    CYGNUS("시그너스", listOf(BossDifficulty.EASY, BossDifficulty.NORMAL), listOf(6, 6)),
    LOTUS("스우",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD, BossDifficulty.EXTREME),
        listOf(6, 6, 2)),
    DAMIEN("데미안",
        listOf(BossDifficulty.NORMAL, BossDifficulty.HARD),
        listOf(6, 6)),
    GUARDIANANGELSLIME("가디언 엔젤 슬라임",
        listOf(BossDifficulty.NORMAL, BossDifficulty.CHAOS),
        listOf(6, 6));

    fun getMaxPartyMemberCount(difficulty: BossDifficulty): Int {
        val index = difficulties.indexOf(difficulty)
        if (index == -1) return 6 // 기본값 (혹은 예외 처리)
        return memberCounts[index]
    }
}