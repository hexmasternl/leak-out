package nl.hexmaster.leakout.core.stack

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.LevelDefaults
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

class DealerTest {
    private val standard = PipeSection.entries.filter { it.mandatory }

    @Test
    fun `first draw follows the weights within one percentage point`() {
        val settings = settings(PipeSection.X to 3, PipeSection.RES to 3)
        val random = Random(42)
        val counts = PipeSection.entries.associateWith { 0 }.toMutableMap()

        repeat(100_000) {
            val (section, _) = Dealer.initial(settings).deal(random)
            counts[section] = counts.getValue(section) + 1
        }

        for (section in PipeSection.entries) {
            val expected = settings.weightOf(section) / 48.0
            val actual = counts.getValue(section) / 100_000.0
            assertTrue(abs(expected - actual) <= 0.01, "$section expected $expected actual $actual")
        }
    }

    @Test
    fun `section with weight 0 is never drawn`() {
        val random = Random(1234)
        var dealer = Dealer.initial(settings(PipeSection.RES to 1))

        repeat(10_000) {
            val (section, next) = dealer.deal(random)
            assertNotEquals(PipeSection.X, section)
            dealer = next
        }
        assertTrue(PipeSection.X !in dealer.weights)
    }

    @Test
    fun `all weights 0 is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Dealer.initial(StackSettings(weights = PipeSection.entries.associateWith { 0 }))
        }
    }

    @Test
    fun `a draw updates the count and the droughts`() {
        val dealer = Dealer.initial(settings())
        assertEquals(0, dealer.draws)

        val (section, next) = dealer.deal(Random(7))

        assertEquals(1, next.draws)
        for (s in standard) {
            assertEquals(if (s == section) 1 else 0, next.dealt.getValue(s), "$s")
            assertEquals(if (s == section) 0 else 1, next.droughts.getValue(s), "$s")
        }
        val (second, afterSecond) = next.deal(Random(8))
        assertEquals(2, afterSecond.draws)
        assertEquals(0, afterSecond.droughts.getValue(second))
        if (second != section) assertEquals(1, afterSecond.droughts.getValue(section))
    }

    @Test
    fun `a section that is behind is more likely and one that is ahead less likely`() {
        val dealer = dealer(
            dealt = mapOf(PipeSection.H to 0, PipeSection.V to 4, PipeSection.LT to 2, PipeSection.LB to 2, PipeSection.RT to 2, PipeSection.RB to 2),
            droughts = mapOf(PipeSection.H to 12, PipeSection.V to 0, PipeSection.LT to 1, PipeSection.LB to 2, PipeSection.RT to 3, PipeSection.RB to 4),
        )
        val chances = dealer.chances()

        assertEquals(12, dealer.draws)
        assertTrue(chances.getValue(PipeSection.H) > 1.0 / 6, "H ${chances.getValue(PipeSection.H)}")
        assertTrue(chances.getValue(PipeSection.V) < 1.0 / 6, "V ${chances.getValue(PipeSection.V)}")
        assertEquals(1.0, chances.values.sum(), 1e-9)
    }

    @Test
    fun `drought limit is 15 with default weights and 18 next to a cross at weight 6`() {
        val defaults = Dealer.initial(settings())
        val withCross = Dealer.initial(settings(PipeSection.X to 6))
        val withSmallCross = Dealer.initial(settings(PipeSection.X to 3))

        for (section in standard) {
            assertEquals(15, defaults.droughtLimit(section), "$section")
            assertEquals(18, withCross.droughtLimit(section), "$section")
            assertEquals(17, withSmallCross.droughtLimit(section), "$section")
        }
    }

    @Test
    fun `section at its drought limit is dealt without drawing`() {
        val dealer = dealer(droughts = standard.associateWith { 1 } + (PipeSection.RB to 15))
        val random = Random(99)

        val (section, next) = dealer.deal(random)

        assertEquals(PipeSection.RB, section)
        assertEquals(0, next.droughts.getValue(PipeSection.RB))
        assertEquals(Random(99).nextDouble(), random.nextDouble(), "forced deal must not consume a random value")
    }

    @Test
    fun `longest drought is forced first and a tie goes to the first section`() {
        val tie = dealer(droughts = standard.associateWith { 1 } + (PipeSection.LT to 16) + (PipeSection.H to 16))
        assertEquals(PipeSection.H, tie.forced())

        val longest = dealer(droughts = standard.associateWith { 1 } + (PipeSection.LT to 17) + (PipeSection.H to 16))
        assertEquals(PipeSection.LT, longest.forced())

        val below = dealer(droughts = standard.associateWith { 14 })
        assertNull(below.forced())
    }

    @Test
    fun `cross and reservoir are never forced`() {
        // 200 draws without a cross or a reservoir: far past any limit a standard section would have.
        val weights = standard.associateWith { 7 } + (PipeSection.X to 1) + (PipeSection.RES to 1)
        val dealer = Dealer(
            weights = weights,
            dealt = weights.mapValues { (s, _) -> if (s.mandatory) 200 / 6 + (if (s == PipeSection.H) 2 else 0) else 0 },
            droughts = weights.mapValues { (s, _) -> if (s.mandatory) 1 else 200 },
        )

        assertNull(dealer.droughtLimit(PipeSection.X))
        assertNull(dealer.droughtLimit(PipeSection.RES))
        assertNull(dealer.forced())
        val random = Random(5)
        dealer.deal(random)
        assertNotEquals(Random(5).nextDouble(), random.nextDouble(), "the next section must be drawn at random")
    }

    @Test
    fun `same seed gives the same sequence`() {
        val settings = settings(PipeSection.X to 3, PipeSection.RES to 1)
        assertEquals(sequence(settings, Random(2026), 200), sequence(settings, Random(2026), 200))
    }

    @Test
    fun `no standard section is missing for more than 15 draws`() {
        repeat(RUNS) { run ->
            val draws = sequence(settings(), Random(run), DRAWS)
            for (section in standard) {
                var gap = 0
                for (drawn in draws) {
                    gap = if (drawn == section) 0 else gap + 1
                    assertTrue(gap <= 15, "run $run: $section missing for $gap draws")
                }
            }
        }
    }

    @Test
    fun `standard sections come out nearly evenly spread with few repeats`() {
        var deviationSum = 0.0
        var repeats = 0
        repeat(RUNS) { run ->
            val draws = sequence(settings(), Random(run), DRAWS)
            val counts = standard.map { section -> draws.count { it == section }.toDouble() }
            val mean = counts.average()
            deviationSum += sqrt(counts.sumOf { (it - mean) * (it - mean) } / counts.size)
            repeats += draws.zipWithNext().count { (a, b) -> a == b }
        }

        val meanDeviation = deviationSum / RUNS
        val repeatShare = repeats.toDouble() / (RUNS * DRAWS)
        assertTrue(meanDeviation <= 0.8, "mean standard deviation $meanDeviation")
        assertTrue(repeatShare <= 0.12, "repeat share $repeatShare")
    }

    @Test
    fun `cross keeps its share over a level`() {
        var crosses = 0
        repeat(RUNS) { run ->
            crosses += sequence(settings(PipeSection.X to 6), Random(run), DRAWS).count { it == PipeSection.X }
        }

        val share = crosses.toDouble() / (RUNS * DRAWS)
        assertTrue(abs(share - 0.125) <= 0.01, "cross share $share")
    }

    private fun sequence(settings: StackSettings, random: Random, count: Int): List<PipeSection> {
        var dealer = Dealer.initial(settings)
        return List(count) {
            val (section, next) = dealer.deal(random)
            dealer = next
            section
        }
    }

    private fun settings(vararg overrides: Pair<PipeSection, Int>): StackSettings =
        StackSettings(weights = LevelDefaults.STACK_WEIGHTS + overrides)

    /** A dealer with default weights in a constructed state. */
    private fun dealer(
        dealt: Map<PipeSection, Int> = standard.associateWith { 2 },
        droughts: Map<PipeSection, Int>,
    ): Dealer = Dealer(standard.associateWith { 7 }, dealt, droughts)

    private companion object {
        const val RUNS = 10_000
        const val DRAWS = 40
    }
}
