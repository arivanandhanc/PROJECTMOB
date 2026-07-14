package `in`.arivanandhan.orubar.core

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import `in`.arivanandhan.orubar.service.UssdAutomatorService

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

    // --- Auto-pay mode (opt-in) -----------------------------------------

    /**
     * Auto-dials *99# via [Intent.ACTION_CALL]. Unlike [dialIntent] this places
     * the call itself, so it requires CALL_PHONE. Used only when the user has
     * opted into "Pay automatically". The accessibility service then fills the
     * menu up to the PIN.
     */
    fun callIntent(): Intent =
        Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(USSD_CODE)))

    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /** True if the user has enabled our USSD auto-fill accessibility service. */
    fun isAutomatorEnabled(context: Context): Boolean {
        val expected = ComponentName(context, UssdAutomatorService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** Opens the system Accessibility settings so the user can enable the service. */
    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

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
