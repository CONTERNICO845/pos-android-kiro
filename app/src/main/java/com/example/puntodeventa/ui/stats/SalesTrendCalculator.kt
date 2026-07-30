package com.example.puntodeventa.ui.stats

import com.example.puntodeventa.data.model.OrderTotalPoint
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Aggregation unit of the sales trend chart. (Req 8.4) */
enum class TrendGranularity(val csvLabel: String) {
    HOURLY("Hora"),
    DAILY("Dia"),
    MONTHLY("Mes")
}

/**
 * One bucket of the sales trend series.
 *
 * @param bucketStartMillis inclusive start of the bucket in the device time zone
 * @param label short X-axis label ("14", "07", "jul 26")
 * @param fullLabel tooltip / CSV label ("14:00 – 14:59", "07/07/2026", "julio 2026")
 * @param revenue revenue accumulated inside the bucket
 */
data class SalesTrendPoint(
    val bucketStartMillis: Long,
    val label: String,
    val fullLabel: String,
    val revenue: Double
)

/**
 * Turns raw (timestamp, amount) order points into a gap-free, ordered trend series.
 *
 * Pure Kotlin (no Android, no I/O) so the bucketing rules are covered by JVM property tests.
 * Bucketing lives here instead of in SQL because SQLite's `strftime` works in UTC seconds and would
 * need manual timezone-offset arithmetic to produce local hours, days and months.
 *
 * Satisfies Requirements: 8.3, 8.4, 8.5
 */
object SalesTrendCalculator {

    private const val ONE_DAY_MS = 86_400_000L

    /** Custom ranges up to this span are charted hour by hour. */
    private const val HOURLY_MAX_SPAN_MS = 2 * ONE_DAY_MS

    /** Custom ranges up to this span are charted day by day. */
    private const val DAILY_MAX_SPAN_MS = 62 * ONE_DAY_MS

    /** Hard stop so a pathological range can never allocate an unbounded series. */
    private const val MAX_BUCKETS = 1_000

    private val SPANISH: Locale = Locale.forLanguageTag("es-MX")
    private val hourLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("HH", SPANISH)
    private val dayLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("dd", SPANISH)
    private val dayFullLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", SPANISH)
    private val monthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yy", SPANISH)
    private val monthFullLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH)

    /**
     * Resolves the bucket unit for a filter/range pair. (Req 8.4)
     *
     * Fixed periods map to a fixed granularity; a custom range is classified by its span so a
     * two-day range stays readable hour by hour while a two-year range collapses to months.
     */
    fun granularityFor(filter: TimeFilter, start: Long, end: Long): TrendGranularity =
        when (filter) {
            TimeFilter.TODAY, TimeFilter.YESTERDAY -> TrendGranularity.HOURLY
            TimeFilter.THIS_MONTH -> TrendGranularity.DAILY
            TimeFilter.ALL -> TrendGranularity.MONTHLY
            TimeFilter.CUSTOM -> {
                val span = (end - start).coerceAtLeast(0L)
                when {
                    span <= HOURLY_MAX_SPAN_MS -> TrendGranularity.HOURLY
                    span <= DAILY_MAX_SPAN_MS -> TrendGranularity.DAILY
                    else -> TrendGranularity.MONTHLY
                }
            }
        }

    /**
     * Builds the series for [granularity] covering `[start, end]`.
     *
     * Every bucket between the first and the last is emitted, including zero-revenue ones, so a quiet
     * hour renders as a gap in the chart instead of disappearing. Points outside the range are
     * ignored; the sum of the returned revenues equals the sum of the accepted amounts. (Req 8.3)
     *
     * For [TrendGranularity.MONTHLY] the series is anchored at the first month that actually holds
     * data rather than at [start] — the "Todo" filter starts at epoch 0, which would otherwise emit
     * hundreds of empty months.
     */
    fun buildSeries(
        granularity: TrendGranularity,
        start: Long,
        end: Long,
        points: List<OrderTotalPoint>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<SalesTrendPoint> {
        if (end < start) return emptyList()

        val inRange = points.filter { it.timestamp in start..end }

        val effectiveStart = if (granularity == TrendGranularity.MONTHLY) {
            inRange.minOfOrNull { it.timestamp }?.coerceAtLeast(start) ?: end
        } else {
            start
        }

        val revenueByBucket = HashMap<Long, Double>()
        inRange.forEach { point ->
            val key = bucketStartOf(point.timestamp, granularity, zone)
            revenueByBucket[key] = (revenueByBucket[key] ?: 0.0) + point.amount
        }

        val series = ArrayList<SalesTrendPoint>()
        var cursor = truncate(effectiveStart, granularity, zone)
        var guard = 0
        while (cursor.toInstant().toEpochMilli() <= end && guard < MAX_BUCKETS) {
            val bucketStart = cursor.toInstant().toEpochMilli()
            series += SalesTrendPoint(
                bucketStartMillis = bucketStart,
                label = shortLabel(cursor, granularity),
                fullLabel = fullLabel(cursor, granularity),
                revenue = revenueByBucket[bucketStart] ?: 0.0
            )
            cursor = advance(cursor, granularity)
            guard++
        }
        return series
    }

    /** Inclusive start of the bucket containing [timestamp]. */
    fun bucketStartOf(timestamp: Long, granularity: TrendGranularity, zone: ZoneId): Long =
        truncate(timestamp, granularity, zone).toInstant().toEpochMilli()

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun truncate(
        millis: Long,
        granularity: TrendGranularity,
        zone: ZoneId
    ): ZonedDateTime {
        val zoned = Instant.ofEpochMilli(millis).atZone(zone)
        return when (granularity) {
            TrendGranularity.HOURLY -> zoned.truncatedTo(ChronoUnit.HOURS)
            TrendGranularity.DAILY -> zoned.toLocalDate().atStartOfDay(zone)
            TrendGranularity.MONTHLY -> zoned.toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
        }
    }

    private fun advance(cursor: ZonedDateTime, granularity: TrendGranularity): ZonedDateTime =
        when (granularity) {
            TrendGranularity.HOURLY -> cursor.plusHours(1)
            // Day/month arithmetic on the local date keeps DST transitions from shifting buckets.
            TrendGranularity.DAILY -> cursor.plusDays(1)
            TrendGranularity.MONTHLY -> cursor.plusMonths(1)
        }

    private fun shortLabel(cursor: ZonedDateTime, granularity: TrendGranularity): String =
        when (granularity) {
            TrendGranularity.HOURLY -> cursor.format(hourLabel)
            TrendGranularity.DAILY -> cursor.format(dayLabel)
            TrendGranularity.MONTHLY -> cursor.format(monthLabel).replace(".", "")
        }

    private fun fullLabel(cursor: ZonedDateTime, granularity: TrendGranularity): String =
        when (granularity) {
            TrendGranularity.HOURLY -> "${cursor.format(hourLabel)}:00 – ${cursor.format(hourLabel)}:59"
            TrendGranularity.DAILY -> cursor.format(dayFullLabel)
            TrendGranularity.MONTHLY -> cursor.format(monthFullLabel).replace(".", "")
        }
}
