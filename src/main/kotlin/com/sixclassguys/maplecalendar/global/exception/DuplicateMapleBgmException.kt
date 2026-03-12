package com.sixclassguys.maplecalendar.global.exception

class DuplicateMapleBgmException(
    override val message: String = "해당 BGM은 이미 존재해요."
) : RuntimeException(message)