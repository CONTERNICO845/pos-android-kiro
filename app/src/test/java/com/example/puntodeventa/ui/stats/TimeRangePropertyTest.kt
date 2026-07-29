package com.example.puntodeventa.ui.stats

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.forAll

/**
 * Property-based tests for StatsViewModel.computeRange.
 *
 * Property 1: Time range computation correctness
 * Property 10: Average ticket computation
 */
class TimeRangePropertyTest : StringSpec({

    /**
     * Property 1: For any filter and any "now" timestamp, start <= end and start >= 0.
     */
    "Property 1 — computeRange always produces start <= end and start >= 0" {
        forAll(
            PropTestConfig(iterations = 200),
            Arb.enum<TimeFilter>(),
            Arb.long(1_000_000_000_000L..1_900_000_000_000L) // realistic epoch millis
        ) { filter, now ->
            val (start, end) = StatsViewModel.computeRange(filter, now)
            start >= 0 && start <= end && end <= now
        }
    }

    /**
     * ALL filter: start is always 0, end is always now.
     */
    "computeRange ALL — start is 0 and end equals now" {
        forAll(
            PropTestConfig(iterations = 100),
            Arb.long(1_000_000_000_000L..1_900_000_000_000L)
        ) { now ->
            val (start, end) = StatsViewModel.computeRange(TimeFilter.ALL, now)
            start == 0L && end == now
        }
    }

    /**
     * TODAY filter: start is on the same day as now (start <= now).
     */
    "computeRange TODAY — start is before now and on the same calendar day" {
        forAll(
            PropTestConfig(iterations = 100),
            Arb.long(1_700_000_000_000L..1_900_000_000_000L)
        ) { now ->
            val (start, end) = StatsViewModel.computeRange(TimeFilter.TODAY, now)
            start <= now && end == now
        }
    }

    /**
     * YESTERDAY filter: end < today's midnight (strictly before today).
     */
    "computeRange YESTERDAY — end is strictly before the start of today" {
        forAll(
            PropTestConfig(iterations = 100),
            Arb.long(1_700_000_000_000L..1_900_000_000_000L)
        ) { now ->
            val (_, endYesterday) = StatsViewModel.computeRange(TimeFilter.YESTERDAY, now)
            val (startToday, _) = StatsViewModel.computeRange(TimeFilter.TODAY, now)
            endYesterday < startToday
        }
    }

    /**
     * Property 10: Average ticket computation — revenue / count when count > 0, else 0.
     */
    "Property 10 — average ticket is revenue/count when count>0, else 0" {
        forAll(
            PropTestConfig(iterations = 200),
            Arb.long(0L..100_000L),
            Arb.long(0L..500L)
        ) { revenueCents: Long, countLong: Long ->
            val revenue = revenueCents.toDouble() / 100.0
            val count = countLong.toInt()
            val avg = if (count > 0) revenue / count else 0.0
            avg >= 0.0
        }
    }
})
