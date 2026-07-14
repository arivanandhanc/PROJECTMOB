package `in`.arivanandhan.orubar.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Builds and fires the *99# dialer intent (blueprint §4.2, §5.4).
 *
 * NON-NEGOTIABLE: uses [Intent.ACTION_DIAL], which only PRE-FILLS the dialer and
 * requires the user to press call. It requires zero permissions and keeps the
 * transaction user-initiated. We never use ACTION_CALL (which auto-dials and
 * needs CALL_PHONE) — that would break the legal position.
 */
object UssdLauncher {

    const val USSD_CODE = "*99#"

    /**
     * Builds `tel:*99%23`. The '#' MUST be percent-encoded to %23 or dialers
     * truncate the string at the hash. [Uri.encode] handles this.
     */
    fun dialIntent(): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(USSD_CODE)))

    /** True if the device exposes the dial action to some app. */
    fun canDial(context: Context): Boolean =
        dialIntent().resolveActivity(context.packageManager) != null

    /**
     * Best-effort dual-SIM detection WITHOUT any phone-state permission. We only
     * read the modem/slot count (which needs no permission), never carrier names.
     * Used to show the "pick the SIM linked to your bank" hint (blueprint §5.4).
     */
    fun isLikelyDualSim(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return false
            val count = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> tm.activeModemCount
                // phoneCount was added in API 23 (M); on 21–22 we can't detect
                // SIM count without a phone-state permission, so we don't try.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    @Suppress("DEPRECATION")
                    tm.phoneCount
                }
                else -> return false
            }
            count >= 2
        } catch (e: Exception) {
            false
        }
    }
}
