package com.example.puntodeventa.ui.stats

/** Direction of a period-over-period change. */
enum class TrendDirection {
    UP,
    DOWN,
    FLAT,

    /** The previous period had no activity, so a percentage would be undefined. */
    NO_BASELINE
}

/**
 * Result of comparing a metric against the previous equivalent period. (Req 13.4 – 13.9)
 *
 * @param percent `(current - previous) / previous * 100`, or `null` when there is no baseline
 * @param available `false` when the selected filter has no comparison period at all ("Todo"),
 *   in which case the metric card hides its indicator instead of showing a meaningless number
 */
data class MetricDelta(
    val direction: TrendDirection,
    val percent: Double?,
    val available: Boolean
) {
    /** Whether the change should be styled as good news (growth or a brand-new metric). */
    val isPositive: Boolean
        get() = direction == TrendDirection.UP || direction == TrendDirection.NO_BASELINE

    companion object {

        /**
         * Smallest baseline that can carry a percentage: half a cent.
         *
         * Every metric compared here is money or a whole count, so anything under half a cent is
         * economically zero. Dividing by such a value would produce an astronomic (or infinite,
         * for subnormal doubles) percentage instead of useful information.
         */
        const val BASELINE_EPSILON = 0.005

        /** Used when the selected period has no previous equivalent window. */
        val UNAVAILABLE = MetricDelta(TrendDirection.FLAT, percent = null, available = false)

        /**
         * Builds the delta for a metric pair. Never returns `NaN` or an infinite percentage:
         * a zero (or economically zero) baseline with activity maps to [TrendDirection.NO_BASELINE]
         * ("Nuevo") and a zero baseline without activity maps to a neutral 0 %.
         */
        fun of(current: Number, previous: Number, hasComparison: Boolean): MetricDelta {
            if (!hasComparison) return UNAVAILABLE

            val currentValue = current.toDouble()
            val previousValue = previous.toDouble()
            if (!currentValue.isFinite() || !previousValue.isFinite()) return UNAVAILABLE

            if (previousValue >= BASELINE_EPSILON) {
                val percent = (currentValue - previousValue) / previousValue * 100.0
                if (percent.isFinite()) {
                    return MetricDelta(
                        direction = when {
                            currentValue > previousValue -> TrendDirection.UP
                            currentValue < previousValue -> TrendDirection.DOWN
                            else -> TrendDirection.FLAT
                        },
                        percent = percent,
                        available = true
                    )
                }
                // Ratio overflowed: fall through and report it as a metric without a usable baseline.
            }

            return if (currentValue >= BASELINE_EPSILON) {
                MetricDelta(TrendDirection.NO_BASELINE, percent = null, available = true)
            } else {
                MetricDelta(TrendDirection.FLAT, percent = 0.0, available = true)
            }
        }
    }
}
