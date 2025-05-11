package com.boardgamegeek.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun utcMillisToLocalDate(utcMillis: Long?): LocalDate? {
    if (utcMillis == null) return null
    return Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
}

fun LocalDate.toUtcMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return this.atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun LocalDate.formatMedium(locale: Locale = Locale.getDefault()): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return this.format(formatter)
}