package `in`.arivanandhan.orubar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Device-only payment history (blueprint §4.4, §5.2). Plain JSON in the app's
 * private files dir. Stores ONLY: UPI ID, payee name, amount, timestamp, and a
 * "dialer opened" flag. Never a PIN, never an account number. Excluded from
 * cloud backup via backup_rules.xml — it must not leave the phone.
 */
class TxnLog(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    data class Entry(
        val vpa: String,
        val name: String?,
        val amount: String?,   // plain string, e.g. "250"
        val timestamp: Long,
        val dialerOpened: Boolean
    )

    @Synchronized
    fun all(): List<Entry> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(
                    vpa = o.getString("vpa"),
                    name = o.optString("name").takeIf { it.isNotEmpty() },
                    amount = o.optString("amount").takeIf { it.isNotEmpty() },
                    timestamp = o.getLong("ts"),
                    dialerOpened = o.optBoolean("dialed", false)
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(entry: Entry) {
        val arr = try {
            if (file.exists()) JSONArray(file.readText()) else JSONArray()
        } catch (e: Exception) {
            JSONArray()
        }
        arr.put(
            JSONObject()
                .put("vpa", entry.vpa)
                .put("name", entry.name ?: "")
                .put("amount", entry.amount ?: "")
                .put("ts", entry.timestamp)
                .put("dialed", entry.dialerOpened)
        )
        // Cap the log so it never grows unbounded on tiny-storage devices.
        val trimmed = if (arr.length() > MAX_ENTRIES) {
            JSONArray().apply {
                for (i in (arr.length() - MAX_ENTRIES) until arr.length()) put(arr.get(i))
            }
        } else arr
        file.writeText(trimmed.toString())
    }

    /** True if this VPA has been paid (or attempted) before — drives the
     *  "first time paying this shop" confirm-screen cue. */
    @Synchronized
    fun hasSeen(vpa: String): Boolean = all().any { it.vpa.equals(vpa, ignoreCase = true) }

    @Synchronized
    fun clear() {
        if (file.exists()) file.delete()
    }

    companion object {
        const val FILE_NAME = "orubar_history.json"
        private const val MAX_ENTRIES = 500
    }
}
