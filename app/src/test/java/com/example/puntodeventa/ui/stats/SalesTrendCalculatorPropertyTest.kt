package com.example.puntodeventa.ui.stats

import com.example.puntodeventa.data.model.OrderTotalPoint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.kotest.property.forAll
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Feature: statistics-dashboard
 * Property 13: Trend granularity selection
 * Property 14: Trend series conservation and ordering
 * Property 15: Trend series bucket assignment
 */
class SalesTrendCalculatorPropertyTest : StringSpec({

    val zone = ZoneId.of("America/Mexico_City")
    val oneDay = 86_400_000L
    // Bounded window so hourly series stay small: 2026-01-01 .. 2027-01-01 approx.
    val arbInstant = Arb.long(1_767_225_600_000L..1_798_761_600_000L)

    /**
     * Property 13: fixed filters map to a fixed granularity; CUSTOM is classified by span.
     */
    "Property 13 — granularityFor maps every filter to the specified bucket unit" {
        forAll(
            PropTestConfig(iterations = 300),
            Arb.enum<TimeFilter>(),
            arbInstant,
            Arb.long(0L..(400L * 86_400_000L))
        ) { filter, start, span ->
            val end = start + span
            val granularity = SalesTrendCalculator.granularityFor(filter, start, end)
            when (filter) {
                TimeFilter.TODAY, TimeFilter.YESTERDAY -> granularity == TrendGranularity.HOURLY
                TimeFilter.THIS_MONTH -> granularity == TrendGranularity.DAILY
                TimeFilter.ALL -> granularity == TrendGranularity.MONTHLY
                TimeFilter.CUSTOM -> when {
                    span <= 2 * 86_400_000L -> granularity == TrendGranularity.HOURLY
                    span <= 62 * 86_400_000L -> granularity == TrendGranularity.DAILY
                    else -> granularity == TrendGranularity.MONTHLY
                }
            }
        }
    }

    /**
     * Property 14: the series is ordered, gap-free, and conserves the revenue of in-range points
     * while ignoring out-of-range ones.
     */
    "Property 14 — buildSeries is ascending, gap-free and conserves in-range revenue" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.enum<TrendGranularity>(),
            arbInstant,
            Arb.list(
                Arb.long(-oneDay..(3 * oneDay)).map { offset -> offset },
                0..25
            ),
            Arb.list(Arb.double(0.01..5_000.0), 0..25)
        ) { granularity, start, offsets, amounts ->
            val span = when (granularity) {
                TrendGranularity.HOURLY -> 2 * oneDay
                TrendGranularity.DAILY -> 40 * oneDay
                TrendGranularity.MONTHLY -> 200 * oneDay
            }
            val end = start + span
            val points = offsets.zip(amounts).map { (offset, amount) ->
                OrderTotalPoint(timestamp = start + offset, amount = amount)
            }

            val series = SalesTrendCalculator.buildSeries(granularity, start, end, points, zone)

            // Ascending and gap-free: each bucket start is the previous cursor advanced once.
            series.zipWithNext().forEach { (a, b) ->
                (b.bucketStartMillis > a.bucketStartMillis) shouldBe true
                val expectedNext = when (granularity) {
                    TrendGranularity.HOURLY ->
                        Instant.ofEpochMilli(a.bucketStartMillis).atZone(zone).plusHours(1)
                    TrendGranularity.DAILY ->
                        Instant.ofEpochMilli(a.bucketStartMillis).atZone(zone).plusDays(1)
                    TrendGranularity.MONTHLY ->
                        Instant.ofEpochMilli(a.bucketStartMillis).atZone(zone).plusMonths(1)
                }.toInstant().toEpochMilli()
                b.bucketStartMillis shouldBe expectedNext
            }

            // Conservation: only points inside [start, end] contribute, and all of them do.
            val expected = points.filter { it.timestamp in start..end }.sumOf { it.amount }
            val actual = series.sumOf { it.revenue }
            (abs(expected - actual) < 0.0001) shouldBe true
        }
    }

    /**
     * Property 15: a point lands in the bucket whose start is the greatest value not greater
     * than its timestamp.
     */
    "Property 15 — every point lands in the bucket containing its timestamp" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.enum<TrendGranularity>(),
            arbInstant,
            Arb.long(0L..(36 * 3_600_000L)),
            Arb.double(1.0..999.0)
        ) { granularity, start, offset, amount ->
            val end = start + 40 * oneDay
            val timestamp = start + offset
            val series = SalesTrendCalculator.buildSeries(
                granularity,
                start,
                end,
                listOf(OrderTotalPoint(timestamp, amount)),
                zone
            )

            val expectedBucket = SalesTrendCalculator.bucketStartOf(timestamp, granularity, zone)
            val hit = series.filter { it.revenue > 0.0 }

            hit.size shouldBe 1
            hit.first().bucketStartMillis shouldBe expectedBucket
            (abs(hit.first().revenue - amount) < 0.0001) shouldBe true
        }
    }

    "buildSeries returns an empty series when the range is inverted" {
        SalesTrendCalculator.buildSeries(
            TrendGranularity.DAILY,
            start = 2_000L,
            end = 1_000L,
            points = emptyList(),
            zone = zone
        ) shouldBe emptyList()
    }

    "buildSeries covers a full day with 24 hourly buckets" {
        val dayStart = Instant.parse("2026-07-30T00:00:00Z").toEpochMilli()
        val start = Instant.ofEpochMilli(dayStart).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + oneDay - 1
        val series = SalesTrendCalculator.buildSeries(
            TrendGranularity.HOURLY,
            start,
            end,
            emptyList(),
            zone
        )
        series.size shouldBe 24
        series.first().label shouldBe "00"
        series.last().label shouldBe "23"
    }

    "MONTHLY anchors on the first month holding data instead of the range start" {
        // "Todo" starts at epoch 0; without anchoring this would emit hundreds of empty months.
        val dataInstant = Instant.parse("2026-06-15T12:00:00Z").toEpochMilli()
        val now = Instant.parse("2026-07-30T12:00:00Z").toEpochMilli()
        val series = SalesTrendCalculator.buildSeries(
            TrendGranularity.MONTHLY,
            start = 0L,
            end = now,
            points = listOf(OrderTotalPoint(dataInstant, 500.0)),
            zone = zone
        )
        series.size shouldBe 2
        series.first().revenue shouldBe 500.0
        series.last().revenue shouldBe 0.0
    }
})
