package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

class PipeStackTest {
    @Test
    fun `new stack holds visible size sections`() {
        for (size in listOf(1, 5, 10)) {
            val stack = PipeStack.create(StackSettings(visibleSize = size), Random(size))
            assertEquals(size, stack.size)
            assertEquals(size, stack.sections.size)
        }
    }

    @Test
    fun `size stays the same after a placement`() {
        val settings = StackSettings(visibleSize = 5)
        val stack = PipeStack.create(settings, Random(1))
        val next = stack.afterPlacement(WeightedPool(settings.weights), Random(2))
        assertEquals(stack.size, next.size)
    }

    @Test
    fun `placement removes bottom and adds new section at top`() {
        val stack = PipeStack(listOf(PipeSection.H, PipeSection.V, PipeSection.LT, PipeSection.LB, PipeSection.RT))
        val pool = WeightedPool(PipeSection.entries.associateWith { if (it == PipeSection.X) 1 else 0 })

        val next = stack.afterPlacement(pool, Random(0))

        assertEquals(listOf(PipeSection.X, PipeSection.H, PipeSection.V, PipeSection.LT, PipeSection.LB), next.sections)
        assertEquals(PipeSection.LB, next.next)
    }

    @Test
    fun `same seed stacks stay equal over placements`() {
        val settings = StackSettings(visibleSize = 5, weights = StackSettings().weights + (PipeSection.X to 1))
        val firstRandom = Random(2026)
        val secondRandom = Random(2026)
        val pool = WeightedPool(settings.weights)
        var first = PipeStack.create(settings, firstRandom)
        var second = PipeStack.create(settings, secondRandom)
        assertEquals(first, second)

        repeat(50) {
            first = first.afterPlacement(pool, firstRandom)
            second = second.afterPlacement(pool, secondRandom)
            assertEquals(first, second)
        }
    }
}
