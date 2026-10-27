package app.sipsure.data

import app.sipsure.domain.AppState
import app.sipsure.domain.CreditLedger
import app.sipsure.domain.HistoryEvent
import app.sipsure.domain.HistoryKind
import app.sipsure.domain.LedgerEntry
import app.sipsure.domain.Phase
import app.sipsure.domain.PolicyState
import app.sipsure.domain.Schedule
import app.sipsure.domain.StateCorruptedException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** JSON (de)serialisation of [AppState]. Any malformed input becomes [StateCorruptedException]. */
object StateCodec {
    private const val VERSION = 1

    fun encode(state: AppState): String = JSONObject().apply {
        put("v", VERSION)
        put("schedule", state.schedule.let {
            JSONObject()
                .put("interval", it.intervalMinutes).put("window", it.checkInWindowMinutes)
                .put("grace", it.graceMinutes).put("quietStart", it.quietStartMinuteOfDay)
                .put("quietEnd", it.quietEndMinuteOfDay)
        })
        put("policy", state.policy.let {
            JSONObject().put("phase", it.phase.name)
                .put("nextDue", it.nextDueAtMs ?: JSONObject.NULL)
                .put("windowEnds", it.windowEndsAtMs ?: JSONObject.NULL)
                .put("graceEnds", it.graceEndsAtMs ?: JSONObject.NULL)
                .put("bypassUntil", it.bypassUntilMs ?: JSONObject.NULL)
        })
        put("ledger", JSONArray(state.ledger.entries.map(::encodeEntry)))
        // Hashes are strings because JSON numbers lose precision beyond 53 bits.
        put("hashes", JSONArray(state.photoHashes.map { it.toString() }))
        put("history", JSONArray(state.history.map { JSONObject().put("at", it.atMs).put("kind", it.kind.name) }))
        put("deviceLock", state.deviceLockEnabled)
    }.toString()

    fun decode(json: String): AppState = try {
        val root = JSONObject(json)
        if (root.getInt("v") != VERSION) throw JSONException("unsupported version")
        val s = root.getJSONObject("schedule")
        val p = root.getJSONObject("policy")
        val schedule = Schedule(
            s.getInt("interval"), s.getInt("window"), s.getInt("grace"), s.getInt("quietStart"), s.getInt("quietEnd"),
        )
        val ledger = CreditLedger.restore(root.getJSONArray("ledger").objects().map(::decodeEntry))
            ?: throw JSONException("inconsistent ledger")
        AppState(
            schedule = schedule,
            policy = PolicyState(
                phase = Phase.valueOf(p.getString("phase")),
                nextDueAtMs = p.nullableLong("nextDue"),
                windowEndsAtMs = p.nullableLong("windowEnds"),
                graceEndsAtMs = p.nullableLong("graceEnds"),
                bypassUntilMs = p.nullableLong("bypassUntil"),
            ),
            ledger = ledger,
            photoHashes = root.getJSONArray("hashes").let { a -> (0 until a.length()).map { a.getString(it).toLong() } },
            history = root.getJSONArray("history").objects()
                .map { HistoryEvent(it.getLong("at"), HistoryKind.valueOf(it.getString("kind"))) },
            deviceLockEnabled = root.optBoolean("deviceLock", false),
        )
    } catch (e: JSONException) {
        throw StateCorruptedException(e)
    } catch (e: IllegalArgumentException) {
        throw StateCorruptedException(e)
    }

    private fun encodeEntry(e: LedgerEntry): JSONObject = JSONObject().put("id", e.txId).put("at", e.atMs).apply {
        when (e) {
            is LedgerEntry.Purchase -> put("t", "purchase").put("credits", e.credits)
            is LedgerEntry.Redemption -> put("t", "redemption").put("credits", e.credits)
            is LedgerEntry.Advance -> put("t", "advance").put("credits", e.credits).put("due", e.dueAtMs)
            is LedgerEntry.Repayment -> put("t", "repayment").put("credits", e.credits).put("advance", e.advanceTxId)
        }
    }

    private fun decodeEntry(o: JSONObject): LedgerEntry {
        val id = o.getString("id")
        val at = o.getLong("at")
        val credits = o.getInt("credits")
        return when (o.getString("t")) {
            "purchase" -> LedgerEntry.Purchase(id, at, credits)
            "redemption" -> LedgerEntry.Redemption(id, at, credits)
            "advance" -> LedgerEntry.Advance(id, at, o.getLong("due"), credits)
            "repayment" -> LedgerEntry.Repayment(id, at, o.getString("advance"), credits)
            else -> throw JSONException("unknown entry type")
        }
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
}
