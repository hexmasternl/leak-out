package nl.hexmaster.leakout.haptics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The system vibrator, whether the device has one or not: nothing throws, and no prompt is needed. */
@RunWith(AndroidJUnit4::class)
class VibratorHapticPlayerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun vibrateAndCancelDoNotThrow() {
        val player = VibratorHapticPlayer(context)
        HapticEffect.entries.forEach { player.vibrate(it.durationMs, it.amplitude) }
        player.cancel()
        player.cancel()
    }

    @Test
    fun theVibratePermissionIsGrantedAtInstall() {
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.VIBRATE))
    }
}
