package nl.hexmaster.leakout.core.stack

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.PipeSection

class WeightedPoolTest {
    @Test
    fun `section with zero weight is never drawn`() {
        val pool = WeightedPool(weights(PipeSection.X to 0, PipeSection.RES to 1))
        val random = Random(1234)

        repeat(10_000) {
            assertNotEquals(PipeSection.X, pool.draw(random))
        }
    }

    @Test
    fun `draw shares stay within one percentage point`() {
        val weights = weights(PipeSection.X to 1, PipeSection.RES to 1)
        val pool = WeightedPool(weights)
        val random = Random(42)
        val counts = PipeSection.entries.associateWith { 0 }.toMutableMap()

        repeat(100_000) {
            val section = pool.draw(random)
            counts[section] = counts.getValue(section) + 1
        }

        val total = weights.values.sum()
        for ((section, weight) in weights) {
            val expected = weight.toDouble() / total
            val actual = counts.getValue(section).toDouble() / 100_000
            assertTrue(abs(expected - actual) <= 0.01, "$section expected $expected actual $actual")
        }
    }

    @Test
    fun `same seed gives the same sequence`() {
        val pool = WeightedPool(weights(PipeSection.X to 1, PipeSection.RES to 1))
        val first = Random(2026)
        val second = Random(2026)

        repeat(100) {
            assertEquals(pool.draw(first), pool.draw(second))
        }
    }

    @Test
    fun `total weight of zero is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            WeightedPool(PipeSection.entries.associateWith { 0 })
        }
    }

    private fun weights(vararg overrides: Pair<PipeSection, Int>): Map<PipeSection, Int> =
        PipeSection.entries.associateWith { section ->
            when (section) {
                PipeSection.H, PipeSection.V -> 3
                PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB -> 2
                PipeSection.X, PipeSection.RES -> 0
            }
        } + overrides
}
