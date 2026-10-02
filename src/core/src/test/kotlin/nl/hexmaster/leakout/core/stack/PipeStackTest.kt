package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

class PipeStackTest {
    @Test
    fun `new stack holds ten sections more than the visible size`() {
        for (size in listOf(1, 5, 10)) {
            val stack = PipeStack.create(StackSettings(visibleSize = size), Random(size))
            assertEquals(size + 10, stack.size)
            assertEquals(size + PipeStack.EXTRA_SECTIONS, stack.sections.size)
        }
    }

    @Test
    fun `default stack holds fifteen sections`() {
        assertEquals(15, PipeStack.create(StackSettings(), Random(0)).size)
    }

    @Test
    fun `new stack numbers its sections from the bottom up`() {
        val stack = PipeStack.create(StackSettings(visibleSize = 5), Random(4))
        assertEquals(0, stack.placements)
        assertEquals(0, stack.idAt(stack.size - 1))
        assertEquals(stack.size - 1, stack.idAt(0))
    }

    @Test
    fun `sections keep their id when they move down`() {
        var stack = PipeStack.create(StackSettings(visibleSize = 5), Random(6))
        val random = Random(7)
        repeat(30) {
            val before = stack
            stack = stack.afterPlacement(random)
            assertEquals(before.placements + 1, stack.placements)
            assertEquals(before.idAt(before.size - 2), stack.idAt(stack.size - 1))
            for (index in 1 until stack.size) {
                assertEquals(before.idAt(index - 1), stack.idAt(index))
            }
            val ids = stack.sections.indices.map(stack::idAt)
            assertEquals(ids.size, ids.toSet().size)
            assertTrue(stack.idAt(0) !in before.sections.indices.map(before::idAt))
        }
    }

    @Test
    fun `new stack is dealt by a fresh dealer`() {
        val stack = PipeStack.create(StackSettings(visibleSize = 5), Random(3))
        assertEquals(15, stack.dealer.draws)
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
            assertEquals(16 + i, stack.dealer.draws)
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
