package nl.hexmaster.leakout.core.update

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateStatusTest {
    @Test
    fun `no available update is none whatever the priority`() {
        assertEquals(UpdateStatus.None, classifyUpdate(available = false, priority = 0))
        assertEquals(UpdateStatus.None, classifyUpdate(available = false, priority = 5))
    }

    @Test
    fun `priority 0 is optional`() {
        assertEquals(UpdateStatus.Optional, classifyUpdate(available = true, priority = 0))
    }

    @Test
    fun `priority 3 is optional`() {
        assertEquals(UpdateStatus.Optional, classifyUpdate(available = true, priority = 3))
    }

    @Test
    fun `priority 4 is required`() {
        assertEquals(UpdateStatus.Required, classifyUpdate(available = true, priority = 4))
    }

    @Test
    fun `priority 5 is required`() {
        assertEquals(UpdateStatus.Required, classifyUpdate(available = true, priority = 5))
    }

    @Test
    fun `out of range priorities fall on the nearest side`() {
        assertEquals(UpdateStatus.Optional, classifyUpdate(available = true, priority = -1))
        assertEquals(UpdateStatus.Required, classifyUpdate(available = true, priority = 9))
    }
}
