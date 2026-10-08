package nl.hexmaster.leakout.audio

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled effects on a real [android.media.SoundPool]: every file opens, loads and plays. */
@RunWith(AndroidJUnit4::class)
class SoundPoolSoundEffectPlayerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val catalog = SoundCatalog.from(context.assets.list(SoundCatalog.ASSET_DIR).orEmpty().asList())

    @Test
    fun loadsAndPlaysEveryBundledEffect() {
        assertEquals(7, catalog.files.size)
        val player = SoundPoolSoundEffectPlayer(context.assets, catalog.files)
        try {
            val deadline = SystemClock.uptimeMillis() + 5_000
            while (!catalog.files.all(player::isLoaded) && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20)
            for (file in catalog.files) {
                assertTrue("$file did not load", player.isLoaded(file))
                player.play(file, 0f)
            }
        } finally {
            player.release()
        }
    }

    @Test
    fun releaseTwiceAndPlayAfterReleaseAreSafe() {
        val player = SoundPoolSoundEffectPlayer(context.assets, catalog.files)
        player.release()
        player.release()
        player.play(catalog.files.first(), 0.5f)
    }
}
