package nl.hexmaster.leakout

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** The manifest side of the daily pipe reminders: permissions and the receivers that restore them. */
class ReminderManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> =
        manifest.getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    private fun Element.name() = getAttributeNS(android, "name")

    @Test
    fun `the app may post notifications and learn about a restart`() {
        val permissions = elements("uses-permission").map { it.name() }.toSet()
        assertTrue("android.permission.POST_NOTIFICATIONS" in permissions)
        assertTrue("android.permission.RECEIVE_BOOT_COMPLETED" in permissions)
    }

    @Test
    fun `the alarm receiver is private to the app`() {
        val receiver = elements("receiver").single { it.name() == ".reminders.ReminderAlarmReceiver" }
        assertEquals("false", receiver.getAttributeNS(android, "exported"))
    }

    @Test
    fun `the restore receiver listens for restart, time zone and clock changes`() {
        val receiver = elements("receiver").single { it.name() == ".reminders.ReminderRestoreReceiver" }
        val actions = receiver.getElementsByTagName("action").let { nodes -> (0 until nodes.length).map { (nodes.item(it) as Element).name() } }
        assertEquals(
            setOf("android.intent.action.BOOT_COMPLETED", "android.intent.action.TIMEZONE_CHANGED", "android.intent.action.TIME_SET"),
            actions.toSet(),
        )
    }

    @Test
    fun `exact alarms are not requested`() {
        val permissions = elements("uses-permission").map { it.name() }
        assertTrue(permissions.none { it.contains("EXACT_ALARM") })
    }
}
