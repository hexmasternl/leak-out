package nl.hexmaster.leakout.ui.designer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

/** Hands an exported level's JSON to the developer. [fileName] is the suggested `level_NNN.json`. */
fun interface LevelExporter {
    fun export(fileName: String, json: String)
}

/**
 * Copies the JSON to the clipboard and opens the share sheet with it as text and [fileName] as the
 * subject, so it can be saved as `assets/levels/level_NNN.json`. Holds the application context only,
 * so the activity-scoped view model that owns it does not leak an activity.
 */
class AndroidLevelExporter(context: Context) : LevelExporter {
    private val context = context.applicationContext

    override fun export(fileName: String, json: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(fileName, json))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        context.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
