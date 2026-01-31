package com.celdy.groufr.ui.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

object ChatDateFormatter {
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    fun format(isoString: String, locale: Locale, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return try {
            val zoned = OffsetDateTime.parse(isoString).atZoneSameInstant(zoneId)
            val today = LocalDate.now(zoneId)
            val daysAgo = ChronoUnit.DAYS.between(zoned.toLocalDate(), today).toInt()
            val time = zoned.toLocalTime().format(TIME_FORMAT)

            when {
                daysAgo == 0 -> time
                daysAgo == 1 -> {
                    val label = if (isCzech(locale)) "Včera" else "Yesterday"
                    "$label $time"
                }
                daysAgo == 2 -> {
                    val label = if (isCzech(locale)) "Předevčírem" else "2 days ago"
                    "$label $time"
                }
                else -> formatFull(zoned, time, locale)
            }
        } catch (exception: DateTimeParseException) {
            ""
        }
    }

    fun formatAbsolute(
        isoString: String,
        locale: Locale,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return try {
            val zoned = OffsetDateTime.parse(isoString).atZoneSameInstant(zoneId)
            formatZoned(zoned, locale, includeYear = true)
        } catch (exception: DateTimeParseException) {
            ""
        }
    }

    fun formatRange(
        startIso: String?,
        endIso: String?,
        locale: Locale,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val start = parseZoned(startIso, zoneId)
        val end = parseZoned(endIso, zoneId)

        if (start == null && end == null) return null
        if (start != null && end == null) return formatZoned(start, locale, includeYear = true)
        if (start == null && end != null) return formatZoned(end, locale, includeYear = true)

        val sameDay = start!!.toLocalDate() == end!!.toLocalDate()
        if (sameDay) {
            val datePart = formatDateOnly(start, locale, includeYear = true)
            val startTime = start.toLocalTime().format(TIME_FORMAT)
            val endTime = end.toLocalTime().format(TIME_FORMAT)
            return "$datePart $startTime - $endTime"
        }

        // Different days - show on two lines
        val startPart = formatZoned(start, locale, includeYear = true)
        val endPart = formatZoned(end, locale, includeYear = true)
        return "$startPart\n$endPart"
    }

    private fun formatFull(zoned: ZonedDateTime, time: String, locale: Locale): String {
        return if (isCzech(locale)) {
            val dayName = CS_DAY_NAMES[zoned.dayOfWeek] ?: ""
            "${dayName} ${zoned.dayOfMonth}.${zoned.monthValue}.${zoned.year} ${time}"
        } else {
            val dayName = EN_DAY_NAMES[zoned.dayOfWeek] ?: ""
            val monthName = EN_MONTH_NAMES[zoned.monthValue] ?: ""
            "${dayName} ${zoned.dayOfMonth} ${monthName} ${zoned.year} ${time}"
        }
    }

    private fun formatZoned(zoned: ZonedDateTime, locale: Locale, includeYear: Boolean): String {
        val time = zoned.toLocalTime().format(TIME_FORMAT)
        return formatDateOnly(zoned, locale, includeYear) + " " + time
    }

    private fun formatDateOnly(zoned: ZonedDateTime, locale: Locale, includeYear: Boolean): String {
        return if (isCzech(locale)) {
            val dayName = CS_DAY_NAMES[zoned.dayOfWeek] ?: ""
            if (includeYear) {
                "${dayName} ${zoned.dayOfMonth}.${zoned.monthValue}.${zoned.year}"
            } else {
                "${dayName} ${zoned.dayOfMonth}.${zoned.monthValue}."
            }
        } else {
            val dayName = EN_DAY_NAMES[zoned.dayOfWeek] ?: ""
            val monthName = EN_MONTH_NAMES[zoned.monthValue] ?: ""
            if (includeYear) {
                "${dayName} ${zoned.dayOfMonth} ${monthName} ${zoned.year}"
            } else {
                "${dayName} ${zoned.dayOfMonth} ${monthName}"
            }
        }
    }

    private fun isCzech(locale: Locale): Boolean {
        return locale.language.equals("cs", ignoreCase = true)
    }

    private fun parseZoned(value: String?, zoneId: ZoneId): ZonedDateTime? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).atZoneSameInstant(zoneId)
        } catch (exception: DateTimeParseException) {
            null
        }
    }

    private val EN_DAY_NAMES = mapOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )

    private val CS_DAY_NAMES = mapOf(
        DayOfWeek.MONDAY to "Po",
        DayOfWeek.TUESDAY to "Út",
        DayOfWeek.WEDNESDAY to "St",
        DayOfWeek.THURSDAY to "Čt",
        DayOfWeek.FRIDAY to "Pá",
        DayOfWeek.SATURDAY to "So",
        DayOfWeek.SUNDAY to "Ne"
    )

    private val EN_MONTH_NAMES = mapOf(
        1 to "Jan",
        2 to "Feb",
        3 to "Mar",
        4 to "Apr",
        5 to "May",
        6 to "Jun",
        7 to "Jul",
        8 to "Aug",
        9 to "Sep",
        10 to "Oct",
        11 to "Nov",
        12 to "Dec"
    )
}
