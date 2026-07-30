package com.example.puntodeventa.ui.stats

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Feature: statistics-dashboard
 * Property 17: Previous range shape
 */
class PreviousRangePropertyTest : StringSpec({

    val zone = ZoneId.of("America/Mexico_City")
    val oneHour = 3_600_000L
    val oneDay = 86_400_000L
    val arbNow = Arb.long(1_767_225_600_000L..1_798_761_600_000L)

    fun customRange(now: Long): Pair<Long, Long> {
        val start = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .minusDays(7)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return Pair(start, start + 3 * oneDay)
    }

    "Property 17 — the previous window never overlaps the current one and keeps its length" {
        checkAll(PropTestConfig(iterations = 300), Arb.enum<TimeFilter>(), arbNow) { filter, now ->
            val (start, end) = if (filter == TimeFilter.CUSTOM) {
                customRange(now)
            } else {
                StatsViewModel.computeRange(filter, now)
            }

            val previous = StatsViewModel.computePreviousRange(filter, start, end, zone)

            if (filter == TimeFilter.ALL) {
                previous shouldBe null
            } else {
                previous.shouldNotBeNull()
                val (previousStart, previousEnd) = previous
                (previousStart <= previousEnd) shouldBe true
                (previousEnd < start) shouldBe true

                when (filter) {
                    // Exact duration for an explicit range.
                    TimeFilter.CUSTOM -> (previousEnd - previousStart) shouldBe (end - start)
                    // Day arithmetic is local-date based, so a DST boundary may differ by one hour.
                    TimeFilter.TODAY, TimeFilter.YESTERDAY ->
                        (abs((previousEnd - previousStart) - (end - start)) <= oneHour) shouldBe true
                    // THIS_MONTH is clamped to avoid overlapping the current month; only the
                    // non-overlap guarantee above applies.
                    else -> Unit
                }
            }
        }
    }

    "TODAY compares against the same clock window of the previous day" {
        val now = Instant.parse("2026-07-30T18:30:00Z").toEpochMilli()
        val (start, end) = StatsViewModel.computeRange(TimeFilter.TODAY, now)
        val (previousStart, previousEnd) =
            StatsViewModel.computePreviousRange(TimeFilter.TODAY, start, end, zone)!!

        Instant.ofEpochMilli(previousStart).atZone(zone).toLocalDate() shouldBe
            Instant.ofEpochMilli(start).atZone(zone).toLocalDate().minusDays(1)
        Instant.ofEpochMilli(previousEnd).atZone(zone).toLocalTime().hour shouldBe
            Instant.ofEpochMilli(end).atZone(zone).toLocalTime().hour
    }

    "THIS_MONTH starts at the first day of the previous month and stops before this month" {
        val now = Instant.parse("2026-03-30T18:00:00Z").toEpochMilli()
        val (start, end) = StatsViewModel.computeRange(TimeFilter.THIS_MONTH, now)
        val (previousStart, previousEnd) =
            StatsViewModel.computePreviousRange(TimeFilter.THIS_MONTH, start, end, zone)!!

        val previousStartDate = Instant.ofEpochMilli(previousStart).atZone(zone).toLocalDate()
        previousStartDate.dayOfMonth shouldBe 1
        previousStartDate shouldBe
            Instant.ofEpochMilli(start).atZone(zone).toLocalDate().minusMonths(1)
        // February is shorter than the elapsed part of March, so the window must be clamped.
        (previousEnd < start) shouldBe true
    }

    "ALL has no baseline" {
        StatsViewModel.computePreviousRange(TimeFilter.ALL, 0L, 1_800_000_000_000L, zone) shouldBe null
    }

    "exportFileName carries the prefix, the timestamp and the csv extension" {
        val name = StatsViewModel.exportFileName(Instant.parse("2026-07-30T18:42:00Z").toEpochMilli())
        name.startsWith("reporte_ventas_") shouldBe true
        name.endsWith(".csv") shouldBe true
        name.removePrefix("reporte_ventas_").removeSuffix(".csv").length shouldBe 15
    }
})
