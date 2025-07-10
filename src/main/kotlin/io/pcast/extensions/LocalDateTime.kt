package io.pcast.extensions

import java.time.LocalDateTime

fun LocalDateTime.minusDays(days: Int): LocalDateTime = minusDays(days.toLong())
