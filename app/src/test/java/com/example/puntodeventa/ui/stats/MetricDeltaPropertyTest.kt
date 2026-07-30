package com.example.puntodeventa.ui.stats

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import io.kotest.property.forAll

/**
 * Feature: statistics-dashboard
 * Property 16: Metric delta correctness
 */
class MetricDeltaPropertyTest : StringSpec({

    val arbAmount = Arb.double(0.0..100_000.0)

    "Property 16 — delta follows the decision table and is never NaN or infinite" {
        checkAll(PropTestConfig(iterations = 300), arbAmount, arbAmount) { current, previous ->
            val delta = MetricDelta.of(current, previous, hasComparison = true)

            delta.available shouldBe true
            (delta.percent?.isFinite() != false) shouldBe true

            when {
                previous >= MetricDelta.BASELINE_EPSILON -> {
                    delta.percent shouldBe (current - previous) / previous * 100.0
                    delta.direction shouldBe when {
                        current > previous -> TrendDirection.UP
                        current < previous -> TrendDirection.DOWN
                        else -> TrendDirection.FLAT
                    }
                }

                current >= MetricDelta.BASELINE_EPSILON -> {
                    delta.direction shouldBe TrendDirection.NO_BASELINE
                    delta.percent shouldBe null
                }

                else -> {
                    delta.direction shouldBe TrendDirection.FLAT
                    delta.percent shouldBe 0.0
                }
            }
        }
    }

    "Property 16b — without a comparison period the delta is always unavailable" {
        forAll(PropTestConfig(iterations = 100), arbAmount, arbAmount) { current, previous ->
            val delta = MetricDelta.of(current, previous, hasComparison = false)
            !delta.available && delta.percent == null
        }
    }

    // ── Decision table, one example per row ──────────────────────────────────

    "growth is UP with a positive percentage" {
        val delta = MetricDelta.of(105.0, 100.0, hasComparison = true)
        delta.direction shouldBe TrendDirection.UP
        delta.percent shouldBe 5.0
        delta.isPositive shouldBe true
        StatsFormatters.formatPercent(delta.percent!!) shouldBe "+5.0%"
    }

    "decline is DOWN with a negative percentage" {
        val delta = MetricDelta.of(97.7, 100.0, hasComparison = true)
        delta.direction shouldBe TrendDirection.DOWN
        StatsFormatters.formatPercent(delta.percent!!) shouldBe "-2.3%"
    }

    "equal values are FLAT and render as the neutral 0.0%" {
        val delta = MetricDelta.of(42.0, 42.0, hasComparison = true)
        delta.direction shouldBe TrendDirection.FLAT
        StatsFormatters.formatPercent(delta.percent!!) shouldBe "0.0%"
    }

    "a zero baseline with activity is NO_BASELINE" {
        val delta = MetricDelta.of(80.0, 0.0, hasComparison = true)
        delta.direction shouldBe TrendDirection.NO_BASELINE
        delta.percent shouldBe null
        delta.isPositive shouldBe true
    }

    "two zeros stay neutral instead of dividing by zero" {
        val delta = MetricDelta.of(0, 0, hasComparison = true)
        delta.direction shouldBe TrendDirection.FLAT
        delta.percent shouldBe 0.0
    }
})
