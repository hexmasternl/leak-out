package nl.hexmaster.leakout.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/** A launcher that launches nothing: JVM tests have no activity to start Play's flows from. */
object NoLauncher : ActivityResultLauncher<IntentSenderRequest>() {
    override val contract: ActivityResultContract<IntentSenderRequest, *>
        get() = error("no contract in JVM tests")

    override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit

    override fun unregister() = Unit
}
