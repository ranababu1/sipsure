package app.sipsure.data

import android.content.Context
import app.sipsure.domain.AppState
import app.sipsure.domain.StateStore

/** Stores app state as one JSON string in app-private preferences (excluded from backup in the manifest). */
class PrefsStateStore(context: Context) : StateStore {
    private val prefs = context.getSharedPreferences("sipsure_state", Context.MODE_PRIVATE)

    override fun load(): AppState? = prefs.getString(KEY, null)?.let(StateCodec::decode)

    override fun save(state: AppState) {
        // commit() so the write is on disk before an alarm or process death can follow.
        check(prefs.edit().putString(KEY, StateCodec.encode(state)).commit()) { "Could not save state" }
    }

    private companion object {
        const val KEY = "state"
    }
}
