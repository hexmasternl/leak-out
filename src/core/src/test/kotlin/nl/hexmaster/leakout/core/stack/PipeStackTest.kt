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
    fun `new stack is dealt by a fresh dealer`() {
        val stack = PipeStack.create(StackSettings(visibleSize = 5), Random(3))
        assertEquals(5, stack.dealer.draws)
        for (section in PipeSection.entries.filter { it.mandatory }) {
            assertEquals(stack.sections.count { it == section }, stack.dealer.dealt.getValue(section), "$section")
        }
    }

    @Test
    fun `size stays the same after a placement`() {
        val stack = PipeStack.create(StackSettings(visibleSize = 5), Random(1))
        val next = stack.afterPlacement(Random(2))
        assertEquals(stack.size, next.size)
    }

    @Test
    fun `placement removes bottom and adds new section at top`() {
        val onlyCross = StackSettings(weights = PipeSection.entries.associateWith { if (it == PipeSection.X) 1 else 0 })
        val stack = PipeStack(listOf(PipeSection.H, PipeSection.V, PipeSection.LT, PipeSection.LB, PipeSection.RT), Dealer.initial(onlyCross))

        val next = stack.afterPlacement(Random(0))

        assertEquals(listOf(PipeSection.X, PipeSection.H, PipeSection.V, PipeSection.LT, PipeSection.LB), next.sections)
        assertEquals(PipeSection.LB, next.next)
    }

    @Test
    fun `the dealer carries over between placements`() {
        val random = Random(5)
        var stack = PipeStack.create(StackSettings(visibleSize = 5), random)

        repeat(20) { i ->
            stack = stack.afterPlacement(random)
            assertEquals(6 + i, stack.dealer.draws)
            assertEquals(0, stack.dealer.droughts.getValue(stack.sections.first()))
        }
    }

    @Test
    fun `same seed stacks stay equal over placements`() {
        val settings = StackSettings(visibleSize = 5, weights = StackSettings().weights + (PipeSection.X to 3))
        val firstRandom = Random(2026)
        val secondRandom = Random(2026)
        var first = PipeStack.create(settings, firstRandom)
        var second = PipeStack.create(settings, secondRandom)
        assertEquals(first, second)

        repeat(50) {
            first = first.afterPlacement(firstRandom)
            second = second.afterPlacement(secondRandom)
            assertEquals(first, second)
        }
    }
}
