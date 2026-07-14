package `in`.arivanandhan.orubar.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.R
import `in`.arivanandhan.orubar.core.UssdLauncher
import `in`.arivanandhan.orubar.core.UssdSession
import `in`.arivanandhan.orubar.data.TxnLog
import `in`.arivanandhan.orubar.databinding.ActivityCheatcardBinding

/**
 * The *99# hand-off (blueprint §5.5). Two ways to complete:
 *
 *  1. **Pay automatically** (opt-in) — with CALL_PHONE + the accessibility
 *     service enabled, the app dials *99# and fills Send Money → UPI ID → VPA →
 *     amount, stopping at the PIN, which the user enters. The PIN is never
 *     handled by the app, and the app has no INTERNET, so nothing leaves the
 *     phone.
 *  2. **Manual** — the guided cheat card + Copy buttons, launching the dialer via
 *     ACTION_DIAL. Always available as a fallback.
 */
class CheatCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheatcardBinding
    private lateinit var vpa: String
    private var name: String? = null
    private lateinit var amount: String

    private val requestCall =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) continueAutoPay()
            else Toast.makeText(this, R.string.autopay_call_denied, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheatcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vpa = intent.getStringExtra(EXTRA_VPA).orEmpty()
        name = intent.getStringExtra(EXTRA_NAME)
        amount = intent.getStringExtra(EXTRA_AMOUNT).orEmpty()

        binding.vpaValue.text = vpa
        binding.amountValue.text = getString(R.string.confirm_rupee, amount)

        if (UssdLauncher.isLikelyDualSim(this)) {
            binding.dualSimHint.visibility = android.view.View.VISIBLE
        }

        binding.btnAutoPay.setOnClickListener { startAutoPay() }
        binding.btnCopyVpa.setOnClickListener { copy(vpa) }
        binding.btnCopyAmount.setOnClickListener { copy(amount) }
        binding.btnOpenDialer.setOnClickListener { openDialerManual() }
        binding.btnDone.setOnClickListener { goHome() }
    }

    // --- Auto-pay ---------------------------------------------------------

    private fun startAutoPay() {
        if (!UssdLauncher.hasCallPermission(this)) {
            requestCall.launch(Manifest.permission.CALL_PHONE)
            return
        }
        continueAutoPay()
    }

    private fun continueAutoPay() {
        if (!UssdLauncher.isAutomatorEnabled(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.autopay_need_a11y_title)
                .setMessage(R.string.autopay_need_a11y_body)
                .setPositiveButton(R.string.autopay_open_settings) { _, _ ->
                    startActivity(UssdLauncher.accessibilitySettingsIntent())
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
            return
        }
        if (!UssdLauncher.canDial(this)) {
            Toast.makeText(this, R.string.cheat_no_dialer, Toast.LENGTH_LONG).show()
            return
        }
        // Arm the accessibility service with this payment, then auto-dial.
        UssdSession.start(vpa, amount)
        logTxn(dialerOpened = true)
        Toast.makeText(this, R.string.autopay_starting, Toast.LENGTH_LONG).show()
        startActivity(UssdLauncher.callIntent())
    }

    // --- Manual fallback --------------------------------------------------

    private fun openDialerManual() {
        if (!UssdLauncher.canDial(this)) {
            Toast.makeText(this, R.string.cheat_no_dialer, Toast.LENGTH_LONG).show()
            return
        }
        copy(vpa) // ready to paste into the *99# box
        logTxn(dialerOpened = true)
        startActivity(UssdLauncher.dialIntent())
    }

    private fun copy(text: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("OruBar", text))
        Toast.makeText(this, R.string.cheat_copied, Toast.LENGTH_SHORT).show()
    }

    private fun logTxn(dialerOpened: Boolean) {
        TxnLog(this).add(
            TxnLog.Entry(
                vpa = vpa,
                name = name,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                dialerOpened = dialerOpened
            )
        )
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_VPA = "vpa"
        const val EXTRA_NAME = "name"
        const val EXTRA_AMOUNT = "amount"
    }
}
